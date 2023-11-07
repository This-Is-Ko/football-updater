package com.ko.footballupdater.controllers;

import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.form.PostsCreationDto;
import com.ko.footballupdater.services.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.ko.footballupdater.utils.PostHelper.generatePostImageSearchUrl;

@Slf4j
@Controller
@RequestMapping(path="/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @GetMapping("")
    public String showPosts(Model model) {
        List<Post> posts = postService.getPosts();

        PostsCreationDto postsCreationDto = new PostsCreationDto();
        for (Post post : posts) {
            generatePostImageSearchUrl(post);
            postsCreationDto.addPost(post);
        }
        model.addAttribute("form", postsCreationDto);
        return "posts";
    }

    @PostMapping("/save")
    public String savePosts(@ModelAttribute PostsCreationDto postsForm) {
        try {
            postService.updatePostPostedStatus(postsForm.getPosts());
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Updating post status failed").setCause(ex).log();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Updating post status failed", ex);
        }
    }
}
