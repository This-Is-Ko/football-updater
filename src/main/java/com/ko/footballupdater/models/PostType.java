package com.ko.footballupdater.models;

/**
 * Enum representing the type of post.
 */
public enum PostType {

    /**
     * Represents a post containing all statistics of player's performance.
     */
    ALL_STAT_POST,

    /**
     * Represents a post containing a standout image as well as all statistics of player's performance.
     */
    STANDOUT_STATS_POST,

    /**
     * Represents a post containing a standout image as well as image(s) with all player performances from the relevant matchday.
     */
    SUMMARY_POST;
}