package com.ko.footballupdater.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.models.QaIssue;
import com.ko.footballupdater.llm.models.QaIssueType;
import com.ko.footballupdater.llm.models.QaVerdict;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.photo.ImageFetcher;
import com.ko.footballupdater.photo.VisionImages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Checks a draft's rendered images and caption with the vision model and stores the verdict on the post.
 * It only flags problems for the reviewer: it never blocks approval or changes the post's images or caption.
 */
@Slf4j
@Service
public class VisionQaService {

    static final int MAX_IMAGES = 10;

    static final String SYSTEM_PROMPT = """
            You check Instagram posts for a football stats account before a human reviewer publishes them.
            You get the post's images in order (image 1 is the first slide) and its caption and stats.
            Report only real, visible problems:
            - TEXT_OVERFLOW: text cut off at an edge, overlapping other text, or unreadable on the background.
            - FACE_CLIPPED: the player's face or head cut off by the crop.
            - STAT_CAPTION_MISMATCH: the caption states something the images or stats contradict.
            - LOW_QUALITY: blurry, pixelated or badly stretched images.
            - WATERMARK: a visible watermark or another publisher's logo.
            - OTHER: anything else a reviewer must see before publishing.
            Set pass to true only when there are no issues.
            """;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private ImageGeneratorProperties imageGeneratorProperties;

    @Autowired
    private ImageFetcher imageFetcher;

    /**
     * Run QA and store the verdict on the post (not saved). Skipped when the LLM is disabled.
     * On failure the verdict fields are cleared so the review page shows QA as not run.
     */
    public void review(Post post) {
        if (!llmClient.isEnabled()) {
            return;
        }
        try {
            List<LlmClient.LlmImage> images = loadImages(post);
            if (images.isEmpty()) {
                log.atInfo().setMessage("No images to QA").addKeyValue("postId", post.getId()).log();
                clear(post);
                return;
            }
            Map<String, Object> postData = PostPromptFormatter.describePost(post, true);
            postData.remove("isNew");
            postData.put("caption", post.getCaption());
            String userPrompt = "Check this post. There are " + images.size() + " images.\n" + PostPromptFormatter.toJson(postData);

            QaVerdict verdict = llmClient.generateStructured(llmProperties.getVisionModel(), SYSTEM_PROMPT, userPrompt, images, QaVerdict.class);
            List<QaIssue> issues = normalise(verdict);
            post.setQaPassed(issues.isEmpty() && verdict != null && !Boolean.FALSE.equals(verdict.pass()));
            post.setQaIssues(OBJECT_MAPPER.writeValueAsString(issues));
            post.setQaCheckedAt(new Date());
            log.atInfo().setMessage("Vision QA " + (post.getQaPassed() ? "passed" : "flagged " + issues.size() + " issues")).addKeyValue("postId", post.getId()).log();
        } catch (Exception ex) {
            log.atWarn().setMessage("Vision QA failed").setCause(ex).addKeyValue("postId", post.getId()).log();
            clear(post);
        }
    }

    static List<QaIssue> normalise(QaVerdict verdict) {
        List<QaIssue> issues = new ArrayList<>();
        if (verdict == null || verdict.issues() == null) {
            return issues;
        }
        for (QaIssue issue : verdict.issues()) {
            if (issue == null) {
                continue;
            }
            String detail = issue.detail() != null ? issue.detail().trim() : "";
            if (detail.length() > 300) {
                detail = detail.substring(0, 300);
            }
            issues.add(new QaIssue(issue.type() != null ? issue.type() : QaIssueType.OTHER, detail));
        }
        return issues;
    }

    private static void clear(Post post) {
        post.setQaPassed(null);
        post.setQaIssues(null);
        post.setQaCheckedAt(null);
    }

    /**
     * The post's images in order, from the local output folder when the file is there, otherwise downloaded.
     */
    List<LlmClient.LlmImage> loadImages(Post post) throws Exception {
        List<LlmClient.LlmImage> images = new ArrayList<>();
        for (String url : post.getImagesUrls().stream().limit(MAX_IMAGES).toList()) {
            BufferedImage image = null;
            String fileName = fileName(url);
            if (fileName != null && imageGeneratorProperties.getOutputPath() != null) {
                File local = new File(imageGeneratorProperties.getOutputPath(), fileName);
                if (local.isFile()) {
                    image = ImageIO.read(local);
                }
            }
            if (image == null) {
                image = imageFetcher.fetchImage(url);
            }
            images.add(VisionImages.toJpeg(image));
        }
        return images;
    }

    private static String fileName(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.isEmpty()) {
                return null;
            }
            String name = path.substring(path.lastIndexOf('/') + 1);
            return name.isEmpty() || name.contains("..") || name.contains("\\") ? null : name;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static List<QaIssue> parseIssues(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return List.of(OBJECT_MAPPER.readValue(json, QaIssue[].class));
        } catch (JsonProcessingException ex) {
            return List.of(new QaIssue(QaIssueType.OTHER, "Unreadable QA result"));
        }
    }
}
