package com.ko.footballupdater.photo.face;

/**
 * The face sidecar is down, timed out or returned something unusable.
 */
public class FaceSidecarException extends Exception {

    public FaceSidecarException(String message) {
        super(message);
    }

    public FaceSidecarException(String message, Throwable cause) {
        super(message, cause);
    }
}
