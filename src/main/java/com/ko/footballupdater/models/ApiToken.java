package com.ko.footballupdater.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Persisted OAuth token for an external platform, so logins survive application restarts.
 * One row per platform.
 */
@Getter
@Setter
@Entity
@Table(name = "api_token")
public class ApiToken {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TokenPlatform platform;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String accessToken;

    /**
     * When the access token expires.
     */
    @Column
    private Date expiresAt;

    /**
     * Refresh token, only used by platforms with a refresh token grant (TikTok).
     */
    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    @Column
    private Date refreshExpiresAt;

    @NotNull
    @Column
    private Date updatedAt = new Date();

    public ApiToken() {
    }

    public ApiToken(TokenPlatform platform) {
        this.platform = platform;
    }
}
