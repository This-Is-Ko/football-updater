package com.ko.footballupdater.photo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Bounding box of the main subject, as fractions of the image width and height (0 to 1, origin top left).
 */
public record SubjectBoxResponse(
        @JsonPropertyDescription("Whether a main subject (the footballer) is visible")
        Boolean subjectFound,
        @JsonPropertyDescription("Left edge of the subject, 0 to 1")
        Double xMin,
        @JsonPropertyDescription("Top edge of the subject (top of the head), 0 to 1")
        Double yMin,
        @JsonPropertyDescription("Right edge of the subject, 0 to 1")
        Double xMax,
        @JsonPropertyDescription("Bottom edge of the subject, 0 to 1")
        Double yMax
) {
}
