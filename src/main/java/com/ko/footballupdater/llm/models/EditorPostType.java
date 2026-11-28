package com.ko.footballupdater.llm.models;

/**
 * Post types the editor agent can pick.
 */
public enum EditorPostType {
    /**
     * Standard post with all stat images.
     */
    ALL_STAT,
    /**
     * Standout image featuring a few stats, followed by the all stat images.
     */
    STANDOUT,
    /**
     * Not posted alone, better grouped into a matchday summary post.
     */
    SUMMARY_MEMBER
}
