package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.Player;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/**
 * Default matcher: trusts photos a reviewer picked by hand and rejects everything else, so no automatically
 * found photo is used until a real matcher (see {@link FaceMatcherSidecarClient}) exists.
 */
@Component
public class NoOpPlayerMatcher implements PlayerMatcher {

    public static final String NAME = "NOOP";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public MatchResult match(Player player, PhotoCandidate candidate, BufferedImage image) {
        if (candidate != null && ManualUrlPhotoSource.NAME.equals(candidate.sourceName())) {
            return MatchResult.pass(1.0, "Photo chosen by the reviewer");
        }
        return MatchResult.fail("No player matcher configured, automatic photos are not used");
    }
}
