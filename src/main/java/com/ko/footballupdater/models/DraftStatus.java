package com.ko.footballupdater.models;

/**
 * Review state of a generated post in the approval queue.
 * Posts created before the queue existed have a null status.
 */
public enum DraftStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}
