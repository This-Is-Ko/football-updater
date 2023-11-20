package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Post;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PrepareStandoutImageDto {

    private Integer postId;

    private Post post;

    private ImageGenParams imageGenParams = new ImageGenParams();

    private List<StatisticEntryGenerateDto> allStats;

    private String error;

}
