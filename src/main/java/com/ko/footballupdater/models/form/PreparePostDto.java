package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
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

    private Post post;

    private String backgroundImageUrl;

    private Boolean forceScaleImage = false;

    private List<StatisticEntryGenerateDto> allStats;

}
