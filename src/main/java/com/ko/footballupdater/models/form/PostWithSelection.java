package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Post;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostWithSelection {

    private Post post;
    private boolean selected;

    public PostWithSelection() {

    }

    public PostWithSelection(Post post, boolean selected) {
        this.post = post;
        this.selected = selected;
    }
}
