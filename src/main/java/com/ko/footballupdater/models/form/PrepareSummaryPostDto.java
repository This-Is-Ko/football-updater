package com.ko.footballupdater.models.form;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PrepareSummaryPostDto {

    private String league;

    private String round;

    private List<PostWithSelection> postWithSelections;

    private String error;

    public void addPostWithSelection(PostWithSelection postWithSelection) {
        if (postWithSelections == null) {
            postWithSelections = new ArrayList<>();
        }
        postWithSelections.add(postWithSelection);
    }

}
