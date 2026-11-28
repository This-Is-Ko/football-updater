package com.ko.footballupdater.services;

import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.UploadDestination;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.form.UploadPostDto;
import com.ko.footballupdater.repositories.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Approval queue actions for generated drafts.
 */
@Slf4j
@Service
public class DraftReviewService {

    private static final int PAGE_SIZE = 50;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    // Posts currently being approved, guards against double submits posting twice
    private final Set<Integer> approvalsInProgress = ConcurrentHashMap.newKeySet();

    public List<Post> getPendingDrafts() {
        return postRepository.findByDraftStatusOrderByDateGeneratedDesc(DraftStatus.PENDING_REVIEW, PageRequest.of(0, PAGE_SIZE));
    }

    public List<Post> getRejectedDrafts() {
        return postRepository.findByDraftStatusOrderByDateGeneratedDesc(DraftStatus.REJECTED, PageRequest.of(0, 20));
    }

    /**
     * Publish the draft to Instagram through the existing upload path, with its images in their current order.
     *
     * @param caption edited caption from the review page, the stored caption is used when blank
     */
    public void approve(Integer postId, String caption) throws Exception {
        if (!approvalsInProgress.add(postId)) {
            throw new IllegalStateException("Post is already being approved");
        }
        try {
            Post post = postService.getPostById(postId);
            if (!DraftStatus.PENDING_REVIEW.equals(post.getDraftStatus())) {
                throw new IllegalStateException("Post is not pending review");
            }
            if (post.isPostedStatus()) {
                throw new IllegalStateException("Post has already been posted");
            }
            List<String> imageUrls = post.getImagesUrls();
            if (imageUrls.isEmpty()) {
                throw new IllegalStateException("Post has no images to upload");
            }

            UploadPostDto uploadPostDto = new UploadPostDto();
            uploadPostDto.setPostId(postId);
            List<ImageUrlEntry> entries = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                entries.add(new ImageUrlEntry(i + 1, imageUrls.get(i)));
            }
            uploadPostDto.setImageUrls(entries);
            uploadPostDto.setCaption(caption != null && !caption.isBlank() ? caption : post.getCaption());
            uploadPostDto.setUploadDestination(UploadDestination.INSTAGRAM);

            // Marks the post as posted and approved on success
            postService.uploadPost(uploadPostDto);
            log.atInfo().setMessage("Draft approved and posted to Instagram").addKeyValue("postId", postId).log();
        } finally {
            approvalsInProgress.remove(postId);
        }
    }

    public void reject(Integer postId) {
        Post post = postService.getPostById(postId);
        if (!DraftStatus.PENDING_REVIEW.equals(post.getDraftStatus())) {
            throw new IllegalStateException("Post is not pending review");
        }
        post.setDraftStatus(DraftStatus.REJECTED);
        postRepository.save(post);
        log.atInfo().setMessage("Draft rejected").addKeyValue("postId", postId).log();
    }

    /**
     * Move a rejected (or editor-skipped) draft back into the queue.
     */
    public void restore(Integer postId) {
        Post post = postService.getPostById(postId);
        if (!DraftStatus.REJECTED.equals(post.getDraftStatus()) || post.isPostedStatus()) {
            throw new IllegalStateException("Only rejected, unposted drafts can be restored");
        }
        post.setDraftStatus(DraftStatus.PENDING_REVIEW);
        postRepository.save(post);
    }
}
