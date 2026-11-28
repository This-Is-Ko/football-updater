package com.ko.footballupdater.photo.face;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.LlmClient;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.photo.PhotoCandidate;
import com.ko.footballupdater.photo.VisionImages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cross-checks a face match with Claude vision. Claude only reads what is written in the photo (jersey number,
 * name on the shirt) and the source caption; it is never asked who the person is from their face.
 * The comparison with the player's details is done here, not by the model.
 */
@Slf4j
@Component
public class IdentityCrossCheck {

    static final String SYSTEM_PROMPT = """
            You read football photos for a fan account's publishing pipeline.
            Do not identify anyone from their face or appearance. Only report text you can actually read in the photo
            and what the source caption says.
            Report the jersey number and the name printed on the shirt of the main subject (the most prominent player),
            or null when they are not clearly readable. Never guess a partly hidden number.
            Then say whether the caption names the expected player, names a different player as the subject, or doesn't say.
            """;

    public enum Verdict { CONSISTENT, CONTRADICTS, UNKNOWN }

    public enum CaptionMention { EXPECTED_PLAYER, DIFFERENT_PLAYER, NOT_STATED }

    public record IdentityReadResponse(
            @JsonPropertyDescription("Jersey number of the main subject as printed, e.g. \"10\", or null if not clearly readable")
            String jerseyNumber,
            @JsonPropertyDescription("Name printed on the main subject's shirt, or null if not clearly readable")
            String nameOnShirt,
            @JsonPropertyDescription("Whether the caption names the expected player, names a different player, or doesn't say")
            CaptionMention captionMention,
            @JsonPropertyDescription("One short sentence on what was read")
            String notes
    ) {
    }

    public record Result(Verdict verdict, List<String> reasons) {
        static Result unknown(String reason) {
            return new Result(Verdict.UNKNOWN, List.of(reason));
        }
    }

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private LlmProperties llmProperties;

    public boolean isAvailable() {
        return llmClient.isEnabled();
    }

    public Result check(Player player, PhotoCandidate candidate, BufferedImage image) {
        if (!llmClient.isEnabled()) {
            return Result.unknown("Jersey and name cross-check skipped (LLM disabled)");
        }
        IdentityReadResponse read;
        try {
            read = llmClient.generateStructured(llmProperties.getVisionModel(), SYSTEM_PROMPT, userPrompt(player, candidate),
                    List.of(VisionImages.toJpeg(image)), IdentityReadResponse.class);
        } catch (Exception ex) {
            log.atWarn().setMessage("Identity cross-check failed").setCause(ex).log();
            return Result.unknown("Jersey and name cross-check failed");
        }
        return compare(player, read);
    }

    static String userPrompt(Player player, PhotoCandidate candidate) {
        String caption = candidate != null && candidate.caption() != null && !candidate.caption().isBlank()
                ? candidate.caption().strip() : "(no caption)";
        if (caption.length() > 1500) {
            caption = caption.substring(0, 1500);
        }
        return "Expected player: " + String.join(" / ", player.getAllPossibleNames()) + "\n"
                + "Source caption:\n<caption>\n" + caption + "\n</caption>\n"
                + "Read the jersey number and shirt name of the main subject, and check the caption.";
    }

    /**
     * Compare what Claude read with the player's shirt number and names.
     */
    static Result compare(Player player, IdentityReadResponse read) {
        if (read == null) {
            return Result.unknown("Jersey and name cross-check returned nothing");
        }
        List<String> contradictions = new ArrayList<>();
        List<String> confirmations = new ArrayList<>();

        Integer readNumber = parseNumber(read.jerseyNumber());
        if (readNumber != null && player.getShirtNumber() != null) {
            if (readNumber.equals(player.getShirtNumber())) {
                confirmations.add("Jersey number " + readNumber + " matches");
            } else {
                contradictions.add("Jersey number " + readNumber + " but " + player.getName() + " wears " + player.getShirtNumber());
            }
        }
        if (read.nameOnShirt() != null && !read.nameOnShirt().isBlank()) {
            if (nameMatches(read.nameOnShirt(), player.getAllPossibleNames())) {
                confirmations.add("Shirt name \"" + read.nameOnShirt().strip() + "\" matches");
            } else {
                contradictions.add("Shirt name \"" + read.nameOnShirt().strip() + "\" doesn't match " + player.getName());
            }
        }
        if (read.captionMention() == CaptionMention.DIFFERENT_PLAYER) {
            contradictions.add("Caption names a different player");
        } else if (read.captionMention() == CaptionMention.EXPECTED_PLAYER) {
            confirmations.add("Caption names " + player.getName());
        }

        if (!contradictions.isEmpty()) {
            return new Result(Verdict.CONTRADICTS, contradictions);
        }
        if (!confirmations.isEmpty()) {
            return new Result(Verdict.CONSISTENT, confirmations);
        }
        return Result.unknown("No readable jersey number, shirt name or caption mention");
    }

    static Integer parseNumber(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.strip();
        if (!digits.matches("\\d{1,2}")) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    /**
     * True when the shirt text is one of the words of any of the player's names (shirts usually show the surname).
     */
    static boolean nameMatches(String shirtName, List<String> playerNames) {
        String shirt = normalise(shirtName);
        if (shirt.isEmpty()) {
            return false;
        }
        for (String name : playerNames) {
            String full = normalise(name);
            if (full.equals(shirt)) {
                return true;
            }
            for (String word : full.split(" ")) {
                if (word.length() > 1 && (word.equals(shirt) || List.of(shirt.split(" ")).contains(word))) {
                    return true;
                }
            }
        }
        return false;
    }

    static String normalise(String text) {
        String stripped = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return stripped.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").strip();
    }
}
