package com.ko.footballupdater.photo;

import com.ko.footballupdater.configuration.PhotoProperties;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.form.ImageGenParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Picks the configured photo sources, matcher and framer, and turns a photo into a framed standout background.
 */
@Slf4j
@Service
public class PhotoPipelineService {

    public static final int STANDOUT_CANVAS_SIZE = 1000;

    /**
     * A photo ready to use as the standout background.
     */
    public record SelectedPhoto(PhotoCandidate candidate, BufferedImage image, ImageGenParams imageGenParams, MatchResult matchResult) {
    }

    @Autowired
    private PhotoProperties photoProperties;

    @Autowired
    private List<PhotoSource> photoSources;

    @Autowired
    private List<PlayerMatcher> playerMatchers;

    @Autowired
    private List<Framer> framers;

    @Autowired
    private ImageFetcher imageFetcher;

    @Autowired
    private ManualUrlPhotoSource manualUrlPhotoSource;

    /**
     * Sources from photo.sources in priority order. Unknown names are skipped.
     */
    public List<PhotoSource> activeSources() {
        List<PhotoSource> active = new ArrayList<>();
        for (String name : photoProperties.getSources()) {
            String trimmed = name.trim().toUpperCase(Locale.ROOT);
            photoSources.stream().filter(source -> source.name().equals(trimmed)).findFirst()
                    .ifPresentOrElse(active::add, () -> log.atWarn().setMessage("Unknown photo source " + trimmed).log());
        }
        return active;
    }

    /**
     * The configured matcher. Falls back to NOOP for unknown names, which never passes automatic photos.
     */
    public PlayerMatcher matcher() {
        String name = photoProperties.getMatcher().trim().toUpperCase(Locale.ROOT);
        return playerMatchers.stream().filter(matcher -> matcher.name().equals(name)).findFirst()
                .orElseGet(() -> {
                    log.atWarn().setMessage("Unknown photo matcher " + name + ", using " + NoOpPlayerMatcher.NAME).log();
                    return playerMatchers.stream().filter(m -> m.name().equals(NoOpPlayerMatcher.NAME)).findFirst().orElseThrow();
                });
    }

    /**
     * The configured framer. Falls back to CENTER for unknown names.
     */
    public Framer framer() {
        String name = photoProperties.getFramer().trim().toUpperCase(Locale.ROOT);
        return framers.stream().filter(framer -> framer.name().equals(name)).findFirst()
                .orElseGet(() -> {
                    log.atWarn().setMessage("Unknown photo framer " + name + ", using " + CenterFramer.NAME).log();
                    return framers.stream().filter(f -> f.name().equals(CenterFramer.NAME)).findFirst().orElseThrow();
                });
    }

    /**
     * Try the configured sources in order and return the first candidate that downloads and passes the matcher.
     */
    public Optional<SelectedPhoto> findAutomaticPhoto(Post post) {
        if (post.getPlayer() == null) {
            return Optional.empty();
        }
        Match match = post.getPlayerMatchPerformanceStats() != null ? post.getPlayerMatchPerformanceStats().getMatch() : null;
        PlayerMatcher matcher = matcher();
        for (PhotoSource source : activeSources()) {
            List<PhotoCandidate> candidates;
            try {
                candidates = source.findCandidates(post.getPlayer(), match);
            } catch (Exception ex) {
                log.atWarn().setMessage("Photo source " + source.name() + " failed").setCause(ex).log();
                continue;
            }
            for (PhotoCandidate candidate : candidates) {
                try {
                    BufferedImage image = imageFetcher.fetchImage(candidate.url());
                    MatchResult result = matcher.match(post.getPlayer(), candidate, image);
                    if (!result.passed()) {
                        log.atInfo().setMessage("Photo candidate rejected by " + matcher.name() + ": " + result.reasons())
                                .addKeyValue("postId", post.getId()).log();
                        continue;
                    }
                    return Optional.of(new SelectedPhoto(candidate, image, framer().frame(image, STANDOUT_CANVAS_SIZE), result));
                } catch (Exception ex) {
                    log.atWarn().setMessage("Photo candidate from " + source.name() + " failed").setCause(ex).addKeyValue("postId", post.getId()).log();
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Download, match and frame a photo URL pasted by the reviewer.
     */
    public SelectedPhoto prepareManualPhoto(Post post, String url, String licence) throws ImageFetchException {
        PhotoCandidate candidate = manualUrlPhotoSource.candidate(url.trim(), licence != null ? licence.trim() : null);
        BufferedImage image = imageFetcher.fetchImage(candidate.url());
        MatchResult result = matcher().match(post.getPlayer(), candidate, image);
        if (!result.passed()) {
            throw new ImageFetchException("Photo rejected: " + String.join(", ", result.reasons()));
        }
        return new SelectedPhoto(candidate, image, framer().frame(image, STANDOUT_CANVAS_SIZE), result);
    }
}
