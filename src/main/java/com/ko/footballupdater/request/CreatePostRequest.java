package com.ko.footballupdater.request;

import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostRequest {

    @NotNull
    private Integer playerId;
    private PlayerMatchPerformanceStats stats;
}