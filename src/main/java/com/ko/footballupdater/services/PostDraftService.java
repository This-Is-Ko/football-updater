package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.FaceMatchingProperties;
import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.CaptionAgent;
import com.ko.footballupdater.llm.EditorAgent;
import com.ko.footballupdater.llm.LlmClient;
import com.ko.footballupdater.llm.VisionQaService;
import com.ko.footballupdater.llm.models.EditorPostType;
import com.ko.footballupdater.llm.models.EditorResult;
import com.ko.footballupdater.llm.models.ValidatedEditorDecision;
import com.ko.footballupdater.llm.models.ValidatedSummaryGroup;
import com.ko.footballupdater.models.CaptionSource;
import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.photo.MatchResult;
import com.ko.footballupdater.photo.PhotoPipelineService;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.utils.PostHelper;
import com.ko.footballupdater.utils.StatFieldHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns newly created posts into drafts for the approval queue: runs the editor agent, generates standout
 * images for STANDOUT picks and writes captions with the caption agent. Every step has a non-LLM fallback,
 * so a failure here never stops posts being saved.
 */
@Slf4j
@Service
public class PostDraftService {

    private static final Map<String, String> SINGULAR_FIELD_NAMES = Map.of(
            "goals", "goal",
            "assists", "assist",
            "yellowCards", "yellowCard",
            "redCards", "redCard"
    );

    @Autowired(required = false)
    private FaceMatchingProperties faceMatchingProperties;

    @Autowired
    private EditorAgent editorAgent;

    @Autowired
    private PhotoPipelineService photoPipelineService;

    @Autowired
    private VisionQaService visionQaService;

    @Autowired
    private CaptionAgent captionAgent;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ImageGeneratorService imageGeneratorService;

    @Autowired
    private AmazonS3Service amazonS3Service;

    @Autowired
    private ImageGeneratorProperties imageGeneratorProperties;

    @Autowired
    private InstagramPostProperies instagramPostProperies;

    @Autowired
    private PostHelper postHelper;

    /**
     * @param newPosts posts created by this player update, already saved so they have ids
     */
    public void processNewPosts(List<Post> newPosts) {
        if (newPosts == null || newPosts.isEmpty()) {
            return;
        }
        for (Post post : newPosts) {
            post.setDraftStatus(DraftStatus.PENDING_REVIEW);
            post.setCaptionSource(CaptionSource.TEMPLATE);
        }

        EditorResult editorResult = editorAgent.decide(newPosts, loadContextPosts(newPosts));
        Map<Integer, String> summaryNotes = summaryGroupNotes(editorResult.summaryGroups(), newPosts);

        boolean llmEnabled = llmClient.isEnabled();
        Set<String> knownNames = llmEnabled ? captionAgent.loadKnownNames() : new HashSet<>();
        int captionsGenerated = 0;

        for (Post post : newPosts) {
            try {
                ValidatedEditorDecision decision = editorResult.decisions().get(post.getId());
                if (decision == null) {
                    continue;
                }
                post.setFeaturedStats(new ArrayList<>(decision.featuredStats()));
                StringBuilder reason = new StringBuilder(decision.reason() != null ? decision.reason() : "");

                if (!decision.include()) {
                    post.setDraftStatus(DraftStatus.REJECTED);
                    post.setLlmReason(("Skipped by editor: " + reason).trim());
                    postRepository.save(post);
                    continue;
                }

                if (EditorPostType.STANDOUT.equals(decision.postType()) && !decision.featuredStats().isEmpty()) {
                    StandoutImageResult imageResult = generateStandoutImage(post, decision.featuredStats());
                    if (imageResult == StandoutImageResult.NOT_GENERATED) {
                        reason.append(" (standout image not generated)");
                    } else if (imageResult == StandoutImageResult.GENERATED_WITH_PHOTO) {
                        reason.append(" (standout image uses a photo from ").append(post.getPhotoSourceName()).append(")");
                    } else if (imageResult == StandoutImageResult.GENERATED_WITH_GENERIC_BASE) {
                        reason.append(" (standout image uses the generic base image, no player standout base image found)");
                    }
                } else if (EditorPostType.SUMMARY_MEMBER.equals(decision.postType())) {
                    reason.append(" (suggested for a summary post)");
                }
                if (summaryNotes.containsKey(post.getId())) {
                    reason.append(" ").append(summaryNotes.get(post.getId()));
                }
                post.setLlmReason(truncate(reason.toString().trim(), 2000));

                if (llmEnabled && captionsGenerated < llmProperties.getMaxPostsPerRun()) {
                    String hashtagSuffix = postHelper.generateCaptionHashtagSuffix(post, instagramPostProperies.getDefaultHashtags());
                    CaptionAgent.CaptionResult captionResult = captionAgent.generateCaption(post, decision.featuredStats(), hashtagSuffix, knownNames);
                    post.setCaption(captionResult.caption());
                    post.setCaptionSource(captionResult.source());
                    captionsGenerated++;
                }
                // Flags problems for the reviewer only, skipped when the LLM is disabled
                if (llmEnabled) {
                    visionQaService.review(post);
                }
                postRepository.save(post);
            } catch (Exception ex) {
                log.atWarn().setMessage("Unable to prepare draft, keeping template caption").setCause(ex).addKeyValue("postId", post.getId()).log();
            }
        }
    }

    private List<Post> loadContextPosts(List<Post> newPosts) {
        try {
            Set<Integer> newIds = newPosts.stream().map(Post::getId).collect(Collectors.toSet());
            int limit = Math.max(1, llmProperties.getMaxPostsPerRun()) * 2;
            return postRepository.findByPostedStatusOrderByDateGeneratedDesc(false, PageRequest.of(0, limit)).stream()
                    .filter(post -> !newIds.contains(post.getId()))
                    .filter(post -> post.getPlayer() != null && post.getPlayerMatchPerformanceStats() != null)
                    .filter(post -> !PostType.SUMMARY_POST.equals(post.getPostType()) && !PostType.SUMMARY_WITH_STANDOUT_IMAGE_POST.equals(post.getPostType()))
                    .filter(post -> !DraftStatus.REJECTED.equals(post.getDraftStatus()))
                    .toList();
        } catch (Exception ex) {
            log.atWarn().setMessage("Unable to load recent unposted posts for the editor").setCause(ex).log();
            return new ArrayList<>();
        }
    }

    private Map<Integer, String> summaryGroupNotes(List<ValidatedSummaryGroup> groups, List<Post> newPosts) {
        Map<Integer, Post> postsById = new HashMap<>();
        newPosts.forEach(post -> postsById.put(post.getId(), post));
        Map<Integer, String> notes = new HashMap<>();
        for (ValidatedSummaryGroup group : groups) {
            for (Integer postId : group.postIds()) {
                if (!postsById.containsKey(postId)) {
                    continue;
                }
                String note = "Summary group suggestion" + (group.name().isEmpty() ? "" : " '" + group.name() + "'")
                        + ": posts " + group.postIds().stream().map(String::valueOf).collect(Collectors.joining(", "))
                        + (group.reason().isEmpty() ? "" : " - " + group.reason());
                notes.merge(postId, note, (a, b) -> a + " " + b);
            }
        }
        return notes;
    }

    enum StandoutImageResult {
        GENERATED,
        GENERATED_WITH_GENERIC_BASE,
        GENERATED_WITH_PHOTO,
        NOT_GENERATED
    }

    /**
     * Generate the standout image with the player standout base image, or the generic base image when that is
     * missing, and add it to the front of the post images.
     */
    StandoutImageResult generateStandoutImage(Post post, List<String> featuredStats) {
        if (!imageGeneratorProperties.isEnabled()) {
            return StandoutImageResult.NOT_GENERATED;
        }
        List<StatisticEntryGenerateDto> selectedStats = selectedStats(post, featuredStats);
        if (selectedStats.isEmpty()) {
            return StandoutImageResult.NOT_GENERATED;
        }
        // A photo from the configured photo sources, if one passes the matcher (none by default)
        Optional<PhotoPipelineService.SelectedPhoto> photo = Optional.empty();
        try {
            photo = photoPipelineService.findAutomaticPhoto(post);
        } catch (Exception ex) {
            log.atWarn().setMessage("Photo pipeline failed, using the base image").setCause(ex).addKeyValue("postId", post.getId()).log();
        }
        if (photo.isPresent()) {
            try {
                renderStandout(post, selectedStats, photo.get());
                return StandoutImageResult.GENERATED_WITH_PHOTO;
            } catch (Exception ex) {
                log.atWarn().setMessage("Unable to generate standout image with photo, using the base image").setCause(ex).addKeyValue("postId", post.getId()).log();
            }
        }
        try {
            // Only upload the new standout image, the stat images are already uploaded
            post.setImagesFileNames(new ArrayList<>());
            boolean usedGenericBase = imageGeneratorService.generateStandoutStatsImage(post, selectedStats, null, true);
            amazonS3Service.uploadToS3(post, true);
            post.setPostType(PostType.STANDOUT_STATS_POST);
            return usedGenericBase ? StandoutImageResult.GENERATED_WITH_GENERIC_BASE : StandoutImageResult.GENERATED;
        } catch (Exception ex) {
            log.atWarn().setMessage("Unable to generate standout image").setCause(ex).addKeyValue("postId", post.getId()).log();
            return StandoutImageResult.NOT_GENERATED;
        }
    }

    /**
     * Render the standout image on a framed photo, put it first in the post images and record the photo source.
     */
    public void renderStandout(Post post, List<StatisticEntryGenerateDto> selectedStats, PhotoPipelineService.SelectedPhoto photo) throws Exception {
        post.setImagesFileNames(new ArrayList<>());
        imageGeneratorService.generateStandoutStatsImage(post, selectedStats, photo.imageGenParams(), false, photo.image());
        amazonS3Service.uploadToS3(post, true);
        post.setPostType(PostType.STANDOUT_STATS_POST);
        post.setPhotoSourceUrl(truncate(photo.candidate().url(), 2000));
        post.setPhotoSourceName(photo.candidate().sourceName());
        post.setPhotoLicence(photo.candidate().licence() != null ? truncate(photo.candidate().licence(), 500) : null);
        applyMatchResult(post, photo.matchResult());
    }

    /**
     * Record the player match on the post, flagging it for the reviewer when the confidence is low.
     */
    public void applyMatchResult(Post post, MatchResult matchResult) {
        if (matchResult == null) {
            post.setPhotoMatchConfidence(null);
            post.setPhotoMatchNotes(null);
            post.setPhotoMatchFlagged(null);
            return;
        }
        double threshold = (faceMatchingProperties != null ? faceMatchingProperties : new FaceMatchingProperties()).getConfidentSimilarity();
        post.setPhotoMatchConfidence(matchResult.confidence());
        post.setPhotoMatchNotes(matchResult.reasons() == null ? null
                : truncate(String.join("\n", matchResult.reasons().stream().map(r -> r.replace('\n', ' ')).toList()), 1000));
        post.setPhotoMatchFlagged(matchResult.confidence() < threshold);
    }

    /**
     * Stat entries for the standout image from the featured stat names, skipping null stats.
     */
    public List<StatisticEntryGenerateDto> selectedStats(Post post, List<String> featuredStats) {
        Map<String, Object> stats = StatFieldHelper.nonNullStats(post.getPlayerMatchPerformanceStats());
        List<StatisticEntryGenerateDto> selectedStats = new ArrayList<>();
        for (String statName : featuredStats) {
            Object value = stats.get(statName);
            if (value == null) {
                continue;
            }
            String displayName = statName;
            if (SINGULAR_FIELD_NAMES.containsKey(statName) && "1".equals(value.toString())) {
                displayName = SINGULAR_FIELD_NAMES.get(statName);
            }
            selectedStats.add(new StatisticEntryGenerateDto(displayName, value.toString(), true));
        }
        return selectedStats;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
