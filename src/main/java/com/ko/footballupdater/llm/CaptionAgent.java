package com.ko.footballupdater.llm;

import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.models.CaptionResponse;
import com.ko.footballupdater.models.AlternativeTeamName;
import com.ko.footballupdater.models.CaptionSource;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.utils.StatFieldHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Writes post captions from the stats, match info and hashtags, in the voice of the few-shot examples.
 * Every caption is checked by {@link CaptionValidator}; on failure it retries once, then keeps the template caption.
 */
@Slf4j
@Service
public class CaptionAgent {

    public record CaptionResult(String caption, CaptionSource source) {
    }

    static final String SYSTEM_PROMPT_TEMPLATE = """
            You write Instagram captions for an account that posts football players' match stat cards.
            The stat images are attached to the post, so the caption is a short hook, not a list of stats.

            Rules:
            - Refer to people and teams only with these placeholders: %s. Never write a player, team or club name yourself.
            - Only use numbers that appear in the post's stats or match data. Don't calculate, round differently or estimate.
            - Don't write hashtags, @mentions or links. Hashtags are added automatically after your caption.
            - Keep it to 1-3 short lines, matching the tone, length and emoji use of the examples.

            Example captions:
            %s
            """;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    /**
     * @param post           post with its template caption already set
     * @param featuredStats  stats the editor chose to feature, may be empty
     * @param hashtagSuffix  hashtags to append after the body (same as the template caption)
     * @param knownNames     every player and team name in the database, from {@link #loadKnownNames()}
     */
    public CaptionResult generateCaption(Post post, List<String> featuredStats, String hashtagSuffix, Set<String> knownNames) {
        CaptionResult templateResult = new CaptionResult(post.getCaption(), CaptionSource.TEMPLATE);
        if (!llmClient.isEnabled() || post.getPlayer() == null || post.getPlayerMatchPerformanceStats() == null) {
            return templateResult;
        }

        Map<String, String> placeholders = placeholders(post);
        Set<String> allowedNumbers = StatFieldHelper.allowedNumbers(post);
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE,
                placeholders.keySet().stream().map(key -> "{" + key + "}").collect(Collectors.joining(", ")),
                String.join("\n---\n", loadExamples()));

        Map<String, Object> postData = PostPromptFormatter.describePost(post, true);
        postData.remove("postId");
        postData.remove("isNew");
        postData.put("featuredStats", featuredStats);
        String basePrompt = "Write the caption for this post:\n" + PostPromptFormatter.toJson(postData);

        String userPrompt = basePrompt;
        for (int attempt = 1; attempt <= 2; attempt++) {
            String body;
            try {
                CaptionResponse response = llmClient.generateStructured(systemPrompt, userPrompt, CaptionResponse.class);
                body = response != null ? response.caption() : null;
            } catch (Exception ex) {
                log.atWarn().setMessage("Caption agent request failed, using template caption").setCause(ex).addKeyValue("player", post.getPlayer().getName()).log();
                return templateResult;
            }

            List<String> errors = CaptionValidator.validate(body, placeholders, allowedNumbers, knownNames, hashtagSuffix);
            if (errors.isEmpty()) {
                String caption = CaptionValidator.render(body, placeholders) + hashtagSuffix;
                log.atInfo().setMessage("Generated LLM caption on attempt " + attempt).addKeyValue("player", post.getPlayer().getName()).log();
                return new CaptionResult(caption, CaptionSource.LLM);
            }
            log.atWarn().setMessage("LLM caption failed validation on attempt " + attempt + ": " + errors).addKeyValue("player", post.getPlayer().getName()).log();
            userPrompt = basePrompt + "\n\nYour previous caption was rejected:\n" + body
                    + "\n\nProblems:\n- " + String.join("\n- ", errors) + "\n\nWrite a new caption that fixes these problems.";
        }
        return templateResult;
    }

    /**
     * Placeholder name to database value for the names a caption may use.
     */
    static Map<String, String> placeholders(Post post) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("player", post.getPlayer().getName());
        String team = PostPromptFormatter.relevantTeam(post);
        if (team != null) {
            placeholders.put("team", team);
        }
        String opponent = PostPromptFormatter.opponentTeam(post);
        if (opponent != null) {
            placeholders.put("opponent", opponent);
        }
        if (post.getPlayerMatchPerformanceStats().getMatch() != null) {
            if (post.getPlayerMatchPerformanceStats().getMatch().getHomeTeamName() != null) {
                placeholders.put("home_team", post.getPlayerMatchPerformanceStats().getMatch().getHomeTeamName());
            }
            if (post.getPlayerMatchPerformanceStats().getMatch().getAwayTeamName() != null) {
                placeholders.put("away_team", post.getPlayerMatchPerformanceStats().getMatch().getAwayTeamName());
            }
        }
        return placeholders;
    }

    /**
     * All player and team names (including alternative team names) in the database.
     */
    public Set<String> loadKnownNames() {
        Set<String> names = new HashSet<>();
        try {
            for (Player player : playerRepository.findAll()) {
                if (player.getName() != null) {
                    names.add(player.getName());
                }
            }
            for (Team team : teamRepository.findAll()) {
                if (team.getName() != null) {
                    names.add(team.getName());
                }
                if (team.getAlternativeTeamNames() != null) {
                    team.getAlternativeTeamNames().stream()
                            .map(AlternativeTeamName::getValue)
                            .filter(value -> value != null)
                            .forEach(names::add);
                }
            }
        } catch (Exception ex) {
            log.atWarn().setMessage("Unable to load player/team names for caption validation").setCause(ex).log();
        }
        return names;
    }

    List<String> loadExamples() {
        Resource resource = resourceLoader.getResource(llmProperties.getCaptionExamplesLocation());
        try (InputStream inputStream = resource.getInputStream()) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return parseExamples(content);
        } catch (IOException ex) {
            log.atWarn().setMessage("Unable to read caption examples from " + llmProperties.getCaptionExamplesLocation()).setCause(ex).log();
            return new ArrayList<>();
        }
    }

    static List<String> parseExamples(String content) {
        String withoutComments = Arrays.stream(content.split("\\R"))
                .filter(line -> !line.startsWith("#"))
                .collect(Collectors.joining("\n"));
        return Arrays.stream(withoutComments.split("(?m)^---\\s*$"))
                .map(String::trim)
                .filter(example -> !example.isEmpty())
                .collect(Collectors.toList());
    }
}
