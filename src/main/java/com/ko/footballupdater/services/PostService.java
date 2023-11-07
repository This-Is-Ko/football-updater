package com.ko.footballupdater.services;


import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.repositories.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;


    public List<Post> getPosts() {
        List<Post> postsList = postRepository.findAllByOrderByDateGeneratedDesc();
        return postsList;
    }

    public void savePosts(List<Post> postsToSave) {
        postsToSave.forEach(post -> postRepository.save(post));
    }

    public void updatePostPostedStatus(List<Post> postsToSave) {
        postsToSave.forEach(post -> postRepository.updatePostSetPostedStatusForId(post.isPostedStatus(), post.getId()));
    }
}
