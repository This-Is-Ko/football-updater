package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.Player;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/**
 * TODO(step 4): face matcher sidecar.
 * Call a face recognition sidecar service that compares the candidate photo with the player's reference
 * images and returns a confidence. Until it's implemented this always fails, so selecting
 * PHOTO_MATCHER=FACE_SIDECAR can never let an unverified photo through.
 */
@Component
public class FaceMatcherSidecarClient implements PlayerMatcher {

    public static final String NAME = "FACE_SIDECAR";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public MatchResult match(Player player, PhotoCandidate candidate, BufferedImage image) {
        // TODO(step 4): send the image and player id to the sidecar and map its response to a MatchResult
        return MatchResult.fail("Face matcher sidecar is not implemented yet");
    }
}
