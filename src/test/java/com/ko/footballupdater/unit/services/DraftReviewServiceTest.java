package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.UploadDestination;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.form.UploadPostDto;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.services.DraftReviewService;
import com.ko.footballupdater.services.PostService;
import com.ko.footballupdater.unit.llm.LlmTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DraftReviewServiceTest {

    @InjectMocks
    private DraftReviewService draftReviewService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostService postService;

    private Post post;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        post = LlmTestData.post(7, "Jamie Rivers");
        post.setDraftStatus(DraftStatus.PENDING_REVIEW);
        post.setImagesUrls(new ArrayList<>(List.of("https://s3/standout.jpg", "https://s3/stats1.jpg", "https://s3/stats2.jpg")));
        when(postService.getPostById(7)).thenReturn(post);
    }

    @Test
    public void approve_pendingDraft_uploadsToInstagramWithImagesInCurrentOrder() throws Exception {
        draftReviewService.approve(7, "Edited caption");

        ArgumentCaptor<UploadPostDto> captor = ArgumentCaptor.forClass(UploadPostDto.class);
        verify(postService).uploadPost(captor.capture());
        UploadPostDto dto = captor.getValue();
        assertEquals(7, dto.getPostId());
        assertEquals("Edited caption", dto.getCaption());
        assertEquals(UploadDestination.INSTAGRAM, dto.getUploadDestination());
        assertEquals(List.of(
                new ImageUrlEntry(1, "https://s3/standout.jpg"),
                new ImageUrlEntry(2, "https://s3/stats1.jpg"),
                new ImageUrlEntry(3, "https://s3/stats2.jpg")), dto.getImageUrls());
    }

    @Test
    public void approve_blankCaption_usesStoredCaption() throws Exception {
        draftReviewService.approve(7, "  ");

        ArgumentCaptor<UploadPostDto> captor = ArgumentCaptor.forClass(UploadPostDto.class);
        verify(postService).uploadPost(captor.capture());
        assertEquals("template caption", captor.getValue().getCaption());
    }

    @Test
    public void approve_notPending_throwsWithoutUploading() throws Exception {
        post.setDraftStatus(DraftStatus.REJECTED);

        assertThrows(IllegalStateException.class, () -> draftReviewService.approve(7, "c"));
        verify(postService, never()).uploadPost(any());
    }

    @Test
    public void approve_alreadyPosted_throwsWithoutUploading() throws Exception {
        post.setPostedStatus(true);

        assertThrows(IllegalStateException.class, () -> draftReviewService.approve(7, "c"));
        verify(postService, never()).uploadPost(any());
    }

    @Test
    public void approve_noImages_throws() throws Exception {
        post.setImagesUrls(new ArrayList<>());

        assertThrows(IllegalStateException.class, () -> draftReviewService.approve(7, "c"));
        verify(postService, never()).uploadPost(any());
    }

    @Test
    public void approve_uploadFails_draftStaysPending() throws Exception {
        org.mockito.Mockito.doThrow(new Exception("Instagram down")).when(postService).uploadPost(any());

        assertThrows(Exception.class, () -> draftReviewService.approve(7, "c"));
        assertEquals(DraftStatus.PENDING_REVIEW, post.getDraftStatus());
        // A later retry isn't blocked by the in-progress guard
        assertThrows(Exception.class, () -> draftReviewService.approve(7, "c"));
        verify(postService, org.mockito.Mockito.times(2)).uploadPost(any());
    }

    @Test
    public void reject_pendingDraft_markedRejected() {
        draftReviewService.reject(7);

        assertEquals(DraftStatus.REJECTED, post.getDraftStatus());
        verify(postRepository).save(post);
    }

    @Test
    public void reject_notPending_throws() {
        post.setDraftStatus(DraftStatus.APPROVED);

        assertThrows(IllegalStateException.class, () -> draftReviewService.reject(7));
        verify(postRepository, never()).save(any());
    }

    @Test
    public void restore_rejectedDraft_backToPending() {
        post.setDraftStatus(DraftStatus.REJECTED);

        draftReviewService.restore(7);

        assertEquals(DraftStatus.PENDING_REVIEW, post.getDraftStatus());
        verify(postRepository).save(post);
    }
}
