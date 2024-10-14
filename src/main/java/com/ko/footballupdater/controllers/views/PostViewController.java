package com.ko.footballupdater.controllers.views;

import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.form.CreatePostDto;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.form.PostWithSelection;
import com.ko.footballupdater.models.form.PostsUpdateDto;
import com.ko.footballupdater.models.form.PrepareStandoutImageDto;
import com.ko.footballupdater.models.form.PrepareSummaryPostDto;
import com.ko.footballupdater.models.form.UploadPostDto;
import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.PlayerService;
import com.ko.footballupdater.services.PostService;
import com.ko.footballupdater.services.TiktokApiService;
import com.ko.footballupdater.utils.MessageType;
import com.ko.footballupdater.utils.PostHelper;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    private TiktokApiService tiktokApiService;

    @Autowired
    private PostHelper postHelper;

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
            postHelper.generatePostImageSearchUrl(post);
            postsUpdateDto.addPost(post);
        }
        model.addAttribute("form", postsUpdateDto);
        model.addAttribute("facebookStatus", facebookApiService.prepareFacebookApiDto());
        model.addAttribute("tiktokStatus", tiktokApiService.prepareTiktokApiDto());
        return "posts";
    }

    /**
     * Save changes to any posts from dashboard posts view
     * @return redirect to posts view
     */
    @PostMapping("/save-all")
    public String saveAllPosts(@ModelAttribute PostsUpdateDto postsForm) {
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
     * Create new post
     * @return create post page
     */
    @GetMapping("/create")
    public String createPost(Model model) {
        try {
            CreatePostDto createPostDto = postService.prepareDtoForCreateNewPost();
            model.addAttribute("postForm", createPostDto);
            return "createPost";
        } catch (Exception ex) {
            log.atError().setMessage("Create post form prep failed").setCause(ex).log();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Create post form prep failed", ex);
        }
    }

    /**
     * Create new post
     * @return create post page
     */
    @PostMapping("/create/submit")
    public String submitCreatePost(@ModelAttribute CreatePostDto createPostDto, RedirectAttributes redirectAttributes) {
        try {
            postService.createPost(createPostDto);
            redirectAttributes.addFlashAttribute("message", "Post created successfully!");
            redirectAttributes.addFlashAttribute("messageType", MessageType.SUCCESS.getType());
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Create post submit failed").setCause(ex).log();
            redirectAttributes.addFlashAttribute("message", "Post creation failed! - " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", MessageType.ERROR.getType());
            return "redirect:/posts";
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
    public String generatePost(Model model, @ModelAttribute PrepareStandoutImageDto prepareStandoutImageForm, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "redirect:/posts";
        }
        try {
            postService.generateStandoutPost(prepareStandoutImageForm);
            redirectAttributes.addFlashAttribute("message", "Post image generate successfully!");
            redirectAttributes.addFlashAttribute("messageType", MessageType.SUCCESS.getType());
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Updating post status failed").setCause(ex).log();
            prepareStandoutImageForm.setError(ex.getMessage());
            model.addAttribute("form", prepareStandoutImageForm);
            model.addAttribute("prepareStandoutImageForm", prepareStandoutImageForm);
            redirectAttributes.addFlashAttribute("message", "Post image generate failed - " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", MessageType.ERROR.getType());
            return "redirect:/posts/prepare-standout-image?postId=" + prepareStandoutImageForm.getPostId();
        }
    }

    /**
     * Display setup to generate summary post
     * @return generate summary image view
     */
    @GetMapping("/prepare/summary")
    public String prepareSummaryPost(Model model) {
        try {
            List<Post> posts = postService.getPosts(null, null, null);
            PrepareSummaryPostDto prepareSummaryPostDto = new PrepareSummaryPostDto();
            for (Post post : posts) {
                if (post.getPostType() != PostType.SUMMARY_POST) {
                    prepareSummaryPostDto.addPostWithSelection(new PostWithSelection(post, false));
                }
            }

            model.addAttribute("form", prepareSummaryPostDto);
            return "prepareSummaryPost";
        } catch (Exception ex) {
            return "error";
        }
    }

    /**
     * Generate post image from selected stats
     * @return redirect to posts view
     */
    @PostMapping("/generate/summary")
    public String generateSummaryPost(Model model, @ModelAttribute PrepareSummaryPostDto prepareSummaryPostDto, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "redirect:/posts";
        }
        try {
            postService.generateSummaryPost(prepareSummaryPostDto);
            redirectAttributes.addFlashAttribute("message", "Summary image generated successfully!");
            redirectAttributes.addFlashAttribute("messageType", MessageType.SUCCESS.getType());
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Updating post status failed").setCause(ex).log();
            prepareSummaryPostDto.setError(ex.getMessage());
            model.addAttribute("form", prepareSummaryPostDto);
            model.addAttribute("prepareStandoutImageForm", prepareSummaryPostDto);
            redirectAttributes.addFlashAttribute("message", "Summary image generate failed! - " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", MessageType.ERROR.getType());
            return "redirect:/posts";
        }
    }

    /**
     * Display page with current post info
     * @return redirect to upload post view
     */
    @GetMapping("/prepare-upload")
    public String prepareUploadPost(Model model, @RequestParam Integer postId, RedirectAttributes redirectAttributes) {
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
            model.addAttribute("tiktokStatus", tiktokApiService.prepareTiktokApiDto());
            return "uploadPost";
        } catch (Exception ex) {
            log.atError().setMessage("Preparing post to upload").setCause(ex).log();
            redirectAttributes.addFlashAttribute("message", "Getting post info failed! - " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", MessageType.ERROR.getType());
            return "redirect:/posts";
        }
    }

    /**
     * Upload images and caption to instagram
     * @return redirect to posts view
     */
    @PostMapping("/upload")
    public String uploadPost(Model model, @ModelAttribute UploadPostDto uploadPostForm, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "redirect:/posts";
        }
        try {
            postService.uploadPost(uploadPostForm);
            redirectAttributes.addFlashAttribute("message", "Upload completed successfully!");
            redirectAttributes.addFlashAttribute("messageType", MessageType.SUCCESS.getType());
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Updating post status failed").setCause(ex).log();
            redirectAttributes.addFlashAttribute("message", "Uploading to Instagram failed! - " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", MessageType.ERROR.getType());
            return "redirect:/posts";
        }
    }

    /**
     * Force update player data
     * @return redirect to upload post view
     */
    @GetMapping("/check-for-new")
    public String checkForNewPosts(Model model, RedirectAttributes redirectAttributes) {
        try {
            playerService.updateDataForAllPlayers();
            redirectAttributes.addFlashAttribute("message", "Checking new player data completed successfully!");
            redirectAttributes.addFlashAttribute("messageType", MessageType.SUCCESS.getType());
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage("Force updating players failed").setCause(ex).log();
            redirectAttributes.addFlashAttribute("message", "Checking new player data failed! - " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", MessageType.ERROR.getType());
            return "redirect:/posts";
        }
    }
}
