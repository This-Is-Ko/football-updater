package com.ko.footballupdater.controllers;


import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.request.CreatePostRequest;
import com.ko.footballupdater.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping(path="/api/post")
public class PostController {

    @Autowired
    PostService postService;

    /**
     * Create new post
     * @return created post
     */
    @PostMapping(path="/create")
    public @ResponseBody Post createPost(@RequestBody CreatePostRequest createPostRequest) {
        try {
            return postService.createPost(createPostRequest);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unable to create new post", ex);
        }
    }
}
