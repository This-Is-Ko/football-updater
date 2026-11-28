package com.ko.footballupdater.llm.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record QaIssue(
        @JsonPropertyDescription("TEXT_OVERFLOW for text cut off or overlapping, FACE_CLIPPED for a face or head cut off by the crop, "
                + "STAT_CAPTION_MISMATCH when the caption disagrees with the image or stats, LOW_QUALITY for blurry or pixelated images, "
                + "WATERMARK for a visible watermark or logo of another publisher, OTHER for anything else")
        QaIssueType type,
        @JsonPropertyDescription("Short description of the problem and which image it is on (image 1, image 2, ...)")
        String detail
) {
}
