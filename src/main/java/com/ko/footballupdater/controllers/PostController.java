package com.ko.footballupdater.controllers;

import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.form.PostsUpdateDto;
import com.ko.footballupdater.models.form.PreparePostDto;
import com.ko.footballupdater.services.PostService;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Null;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.ko.footballupdater.utils.PostHelper.generatePostImageSearchUrl;

@Slf4j
@Controller
@RequestMapping(path="/posts")
public class PostController {

    @Autowired
    private PostService postService;

    /**
     * Display all generated posts
     * @return posts view
     */
    @GetMapping("")
    public String getPosts(Model model, @RequestParam @Nullable Boolean postedStatus) {
        List<Post> posts = postService.getPosts(postedStatus);

        PostsUpdateDto postsUpdateDto = new PostsUpdateDto();
        for (Post post : posts) {
            generatePostImageSearchUrl(post);
            postsUpdateDto.addPost(post);
        }
        model.addAttribute("form", postsUpdateDto);
        return "posts";
    }

    /**
     * Save changes to any posts from dashboard posts view
     * @return redirect to posts view
     */
    @PostMapping("/save")
    public String savePosts(@ModelAttribute PostsUpdateDto postsForm) {
        try {
            postService.updatePostPostedStatus(postsForm.getPosts());
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Updating post status failed").setCause(ex).log();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Updating post status failed", ex);
        }
    }

    /**
     * Delete selected post
     * @return redirect to posts view
     */
    @GetMapping("/delete")
    public String deletePost(@RequestParam Integer postId) {
        try {
            postService.deletePost(postId);
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Deleting post status failed").setCause(ex).log();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Deleting post status failed", ex);
        }
    }

    /**
     * Display setup to generate post
     * @return generate post view
     */
    @GetMapping("/prepare")
    public String preparePost(Model model, @RequestParam Integer postId) {
        try {
            PreparePostDto preparePostDto = postService.prepareDtoForGeneratePost(postId);
            preparePostDto.setPostId(postId);
            model.addAttribute("form", preparePostDto);
            model.addAttribute("preparePostForm", preparePostDto);
            return "preparePost";
        } catch (Exception ex) {
            return "error";
        }
    }

    /**
     * Generate post image from selected stats
     * @return redirect to posts view
     */
    @PostMapping("/generate")
    public String generatePost(Model model, @ModelAttribute PreparePostDto preparePostForm, BindingResult result) {
        if (result.hasErrors()) {
            return "redirect:/posts";
        }
        try {
            postService.generateStandoutPost(preparePostForm);
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Updating post status failed").setCause(ex).log();
            preparePostForm.setError(ex.getMessage());
            model.addAttribute("form", preparePostForm);
            model.addAttribute("preparePostForm", preparePostForm);
            return "redirect:/posts/prepare?postId=" + preparePostForm.getPostId();
        }
    }
}
