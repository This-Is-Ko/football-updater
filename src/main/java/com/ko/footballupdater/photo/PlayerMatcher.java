package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.Player;

import java.awt.image.BufferedImage;

/**
 * Checks a candidate photo shows the player. Selected with photo.matcher (PHOTO_MATCHER).
 * A photo is only used when the match passes.
 */
public interface PlayerMatcher {

    String name();

    MatchResult match(Player player, PhotoCandidate candidate, BufferedImage image);
}
