package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class PreparePostDto {

    private Integer postId;

    private String backgroundImageUrl;

    private HashMap<String, String> availableStatMap;

    private List<StatisticEntryGenerateDto> allStats;

}
