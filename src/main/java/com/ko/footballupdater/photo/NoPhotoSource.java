package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default source: no automatic photos, so the player or generic base image is used.
 */
@Component
public class NoPhotoSource implements PhotoSource {

    public static final String NAME = "NONE";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<PhotoCandidate> findCandidates(Player player, Match match) {
        return List.of();
    }
}
