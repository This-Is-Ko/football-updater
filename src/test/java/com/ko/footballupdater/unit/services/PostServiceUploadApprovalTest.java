package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.UploadDestination;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.form.UploadPostDto;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.PostService;
import com.ko.footballupdater.unit.llm.LlmTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostServiceUploadApprovalTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private FacebookApiService facebookApiService;

    private Post post;
    private UploadPostDto dto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        post = LlmTestData.post(5, "Jamie Rivers");
        when(postRepository.findById(5)).thenReturn(Optional.of(post));
        dto = new UploadPostDto();
        dto.setPostId(5);
        dto.setCaption("caption");
        dto.setUploadDestination(UploadDestination.INSTAGRAM);
        dto.setImageUrls(List.of(new ImageUrlEntry(1, "https://s3/a.jpg")));
    }

    @Test
    public void uploadPost_pendingDraft_markedPostedAndApproved() throws Exception {
        post.setDraftStatus(DraftStatus.PENDING_REVIEW);

        postService.uploadPost(dto);

        assertTrue(post.isPostedStatus());
        assertEquals(DraftStatus.APPROVED, post.getDraftStatus());
        verify(postRepository).save(post);
    }

    @Test
    public void uploadPost_legacyPost_draftStatusStaysNull() throws Exception {
        postService.uploadPost(dto);

        assertTrue(post.isPostedStatus());
        assertNull(post.getDraftStatus());
    }

    @Test
    public void uploadPost_instagramFails_notMarkedPostedOrApproved() throws Exception {
        post.setDraftStatus(DraftStatus.PENDING_REVIEW);
        doThrow(new Exception("Instagram down")).when(facebookApiService).postToInstagram(any(), anyList());

        assertThrows(Exception.class, () -> postService.uploadPost(dto));

        assertFalse(post.isPostedStatus());
        assertEquals(DraftStatus.PENDING_REVIEW, post.getDraftStatus());
        verify(postRepository, never()).save(any());
    }
}
