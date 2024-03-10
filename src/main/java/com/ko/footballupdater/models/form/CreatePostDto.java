package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CreatePostDto {

    private Integer selectedPlayerId;

    private List<CreatePostPlayerDto> players;

    private PlayerMatchPerformanceStats playerMatchPerformanceStats;

    private Date formattedMatchDate;

}
