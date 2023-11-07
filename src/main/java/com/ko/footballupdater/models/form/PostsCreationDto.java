package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Post;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class PostsCreationDto {
    private List<Post> posts;

    public void addPost(Post post) {
        if (posts == null) {
            posts = new ArrayList<>();
        }
        posts.add(post);
    }
}
