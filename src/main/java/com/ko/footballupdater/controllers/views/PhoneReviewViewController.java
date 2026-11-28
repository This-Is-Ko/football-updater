package com.ko.footballupdater.controllers.views;

import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.services.DraftReviewService;
import com.ko.footballupdater.services.ReviewLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Phone review page opened from the signed link in the update email. Opening the link only shows the draft,
 * so mail scanners that prefetch links can't approve anything; Approve and Reject are form posts.
 * No dashboard login is needed: the signed, single-use token is the credential.
 */
@Slf4j
@Controller
@RequestMapping(path = "/review")
public class PhoneReviewViewController {

    private static final String VIEW = "reviewPhone";

    @Autowired
    private ReviewLinkService reviewLinkService;

    @Autowired
    private DraftReviewService draftReviewService;

    @GetMapping("/{token}")
    public String view(@PathVariable("token") String token, Model model) {
        Optional<Post> post = reviewLinkService.resolve(token);
        if (post.isEmpty()) {
            return result(model, "This link has expired or was already used.", false);
        }
        model.addAttribute("post", post.get());
        model.addAttribute("token", token);
        return VIEW;
    }

    @PostMapping("/{token}/approve")
    public String approve(@PathVariable("token") String token,
                          @RequestParam(value = "caption", required = false) String caption, Model model) {
        Optional<Post> post = reviewLinkService.consume(token);
        if (post.isEmpty()) {
            return result(model, "This link has expired or was already used.", false);
        }
        try {
            draftReviewService.approve(post.get().getId(), caption);
            return result(model, "Approved and posted to Instagram.", true);
        } catch (Exception ex) {
            log.atError().setMessage("Approving draft from phone link failed").setCause(ex).addKeyValue("postId", post.get().getId()).log();
            // Keep the link usable so it can be retried
            reviewLinkService.restore(post.get(), token);
            model.addAttribute("retryToken", token);
            return result(model, "Approving failed - " + ex.getMessage(), false);
        }
    }

    @PostMapping("/{token}/reject")
    public String reject(@PathVariable("token") String token, Model model) {
        Optional<Post> post = reviewLinkService.consume(token);
        if (post.isEmpty()) {
            return result(model, "This link has expired or was already used.", false);
        }
        try {
            draftReviewService.reject(post.get().getId());
            return result(model, "Rejected. It's in the recently rejected list on the dashboard if you change your mind.", true);
        } catch (Exception ex) {
            log.atError().setMessage("Rejecting draft from phone link failed").setCause(ex).addKeyValue("postId", post.get().getId()).log();
            return result(model, "Rejecting failed - " + ex.getMessage(), false);
        }
    }

    private String result(Model model, String message, boolean success) {
        model.addAttribute("message", message);
        model.addAttribute("messageType", success ? "success" : "error");
        model.addAttribute("dashboardUrl", reviewLinkService.reviewQueueUrl());
        return VIEW;
    }
}
