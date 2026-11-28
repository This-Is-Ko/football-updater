package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;

import java.util.List;

/**
 * Finds candidate photos of a player from a match. Selected with photo.sources (PHOTO_SOURCES), in priority order.
 */
public interface PhotoSource {

    /**
     * @return the name used in photo.sources
     */
    String name();

    List<PhotoCandidate> findCandidates(Player player, Match match);
}
