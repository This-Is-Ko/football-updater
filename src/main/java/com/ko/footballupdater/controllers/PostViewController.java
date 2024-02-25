package com.ko.footballupdater.controllers;

import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.form.PostsUpdateDto;
import com.ko.footballupdater.models.form.PrepareStandoutImageDto;
import com.ko.footballupdater.models.form.UploadPostDto;
import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.PlayerService;
import com.ko.footballupdater.services.PostService;
import jakarta.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.List;

import static com.ko.footballupdater.utils.PostHelper.generatePostImageSearchUrl;

@Slf4j
@Controller
@RequestMapping(path="/posts")
public class PostViewController {

    @Autowired
    private PostService postService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private FacebookApiService facebookApiService;

    /**
     * Display all generated posts
     * @return posts view
     */
    @GetMapping("")
    public String getPosts(@RequestParam("page") @Nullable Integer pageNumber,
                           @RequestParam("size") @Nullable Integer pageSize,
                           Model model,
                           @RequestParam @Nullable Boolean postedStatus
    ) {
        List<Post> posts = postService.getPosts(postedStatus, pageNumber, pageSize);

        PostsUpdateDto postsUpdateDto = new PostsUpdateDto();
        for (Post post : posts) {
            generatePostImageSearchUrl(post);
            postsUpdateDto.addPost(post);
        }
        model.addAttribute("form", postsUpdateDto);
        model.addAttribute("facebookStatus", facebookApiService.prepareFacebookApiDto());
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
     * Display setup to generate standout iamge
     * @return generate standout image view
     */
    @GetMapping("/prepare-standout-image")
    public String preparePostStandoutImage(Model model, @RequestParam Integer postId) {
        try {
            PrepareStandoutImageDto prepareStandoutImageDto = postService.prepareDtoForGeneratePost(postId);
            prepareStandoutImageDto.setPostId(postId);
            model.addAttribute("form", prepareStandoutImageDto);
            model.addAttribute("prepareStandoutImageForm", prepareStandoutImageDto);
            return "prepareStandoutImage";
        } catch (Exception ex) {
            return "error";
        }
    }

    /**
     * Generate post image from selected stats
     * @return redirect to posts view
     */
    @PostMapping("/generate")
    public String generatePost(Model model, @ModelAttribute PrepareStandoutImageDto prepareStandoutImageForm, BindingResult result) {
        if (result.hasErrors()) {
            return "redirect:/posts";
        }
        try {
            postService.generateStandoutPost(prepareStandoutImageForm);
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Updating post status failed").setCause(ex).log();
            prepareStandoutImageForm.setError(ex.getMessage());
            model.addAttribute("form", prepareStandoutImageForm);
            model.addAttribute("prepareStandoutImageForm", prepareStandoutImageForm);
            return "redirect:/posts/prepare?postId=" + prepareStandoutImageForm.getPostId();
        }
    }

    /**
     * Display page with current post info
     * @return redirect to upload post view
     */
    @GetMapping("/prepare-upload")
    public String prepareUploadPost(Model model, @RequestParam Integer postId) {
        try {
            UploadPostDto uploadPostDto = new UploadPostDto();
            uploadPostDto.setPost(postService.getPostById(postId));
            uploadPostDto.setPostId(postId);
            List<ImageUrlEntry> imageUrls = new ArrayList<>();
            for (int i = 0; i < uploadPostDto.getPost().getImagesUrls().size(); i++) {
                imageUrls.add(new ImageUrlEntry(i+1, uploadPostDto.getPost().getImagesUrls().get(i)));
            }
            uploadPostDto.setImageUrls(imageUrls);
            model.addAttribute("form", uploadPostDto);
            model.addAttribute("uploadPostForm", uploadPostDto);
            model.addAttribute("facebookStatus", facebookApiService.prepareFacebookApiDto());
            return "uploadPost";
        } catch (Exception ex) {
            log.atError().setMessage("Preparing post to upload").setCause(ex).log();
            return "redirect:/posts";
        }
    }

    /**
     * Upload images and caption to instagram
     * @return redirect to posts view
     */
    @PostMapping("/upload")
    public String uploadPost(Model model, @ModelAttribute UploadPostDto uploadPostForm, BindingResult result) {
        if (result.hasErrors()) {
            return "redirect:/posts";
        }
        try {
            postService.uploadPost(uploadPostForm);
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Updating post status failed").setCause(ex).log();
            return "redirect:/posts";
        }
    }

    /**
     * Force update player data
     * @return redirect to upload post view
     */
    @GetMapping("/check-for-new")
    public String checkForNewPosts(Model model) {
        try {
            playerService.updateDataForAllPlayers();
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Force updating players failed").setCause(ex).log();
            return "redirect:/posts";
        }
    }
}
