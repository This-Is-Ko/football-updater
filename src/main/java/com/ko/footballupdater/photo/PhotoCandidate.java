package com.ko.footballupdater.photo;

import java.util.Date;

/**
 * A photo that might be used as a standout image background.
 *
 * @param licence free-text licence or credit, as given by the source or the reviewer
 */
public record PhotoCandidate(String url, String sourceName, String licence, String caption, Date takenAt) {
}
