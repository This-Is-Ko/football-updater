package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Post;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UploadPostDto {

    private Integer postId;

    private Post post;

    private List<ImageUrlEntry> imageUrls = new ArrayList<>();

}
