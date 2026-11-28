package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Photos pasted as a URL on the review page. It never finds photos by itself.
 */
@Component
public class ManualUrlPhotoSource implements PhotoSource {

    public static final String NAME = "MANUAL_URL";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<PhotoCandidate> findCandidates(Player player, Match match) {
        return List.of();
    }

    public PhotoCandidate candidate(String url, String licence) {
        return new PhotoCandidate(url, NAME, licence, null, new Date());
    }
}
