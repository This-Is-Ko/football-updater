package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.UploadDestination;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.form.UploadPostDto;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.services.DraftReviewService;
import com.ko.footballupdater.services.PostService;
import com.ko.footballupdater.services.PostDraftService;
import com.ko.footballupdater.services.PerformanceAnalysisService;
import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.llm.VisionQaService;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.form.ImageGenParams;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.photo.ImageFetchException;
import com.ko.footballupdater.photo.MatchResult;
import com.ko.footballupdater.photo.PhotoCandidate;
import com.ko.footballupdater.photo.PhotoPipelineService;
import java.awt.image.BufferedImage;
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
import static org.mockito.ArgumentMatchers.anyList;
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

    @Mock
    private PostDraftService postDraftService;

    @Mock
    private PhotoPipelineService photoPipelineService;

    @Mock
    private VisionQaService visionQaService;

    @Mock
    private PerformanceAnalysisService performanceAnalysisService;

    @Mock
    private ImageGeneratorProperties imageGeneratorProperties;

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

    private PhotoPipelineService.SelectedPhoto manualPhoto() {
        return new PhotoPipelineService.SelectedPhoto(new PhotoCandidate("https://photos.example/p.jpg", "MANUAL_URL", "Own photo", null, null),
                new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), new ImageGenParams(), MatchResult.pass(1.0, "manual"));
    }

    @Test
    public void usePhotoUrl_pendingStandoutDraft_replacesStandoutAndReRunsQa() throws Exception {
        when(imageGeneratorProperties.isEnabled()).thenReturn(true);
        post.setPostType(PostType.STANDOUT_STATS_POST);
        post.setImagesUrls(new ArrayList<>(List.of("https://s3/JamieRivers_20261122_standout_stat_image_1.jpg", "https://s3/stats1.jpg")));
        post.setFeaturedStats(List.of("goals"));
        List<StatisticEntryGenerateDto> stats = List.of(new StatisticEntryGenerateDto("goals", "2", true));
        when(postDraftService.selectedStats(post, List.of("goals"))).thenReturn(stats);
        PhotoPipelineService.SelectedPhoto photo = manualPhoto();
        when(photoPipelineService.prepareManualPhoto(post, "https://photos.example/p.jpg", "Own photo")).thenReturn(photo);

        draftReviewService.usePhotoUrl(7, "https://photos.example/p.jpg", "Own photo");

        // The old standout image is removed before the new one is added to the front
        assertEquals(List.of("https://s3/stats1.jpg"), post.getImagesUrls());
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(postDraftService, visionQaService, postRepository);
        inOrder.verify(postDraftService).renderStandout(post, stats, photo);
        inOrder.verify(visionQaService).review(post);
        inOrder.verify(postRepository).save(post);
    }

    @Test
    public void usePhotoUrl_noFeaturedStats_usesRuleBasedHighlights() throws Exception {
        when(imageGeneratorProperties.isEnabled()).thenReturn(true);
        when(performanceAnalysisService.highlightStatFields(post)).thenReturn(List.of("goals", "assists"));
        when(postDraftService.selectedStats(post, List.of("goals", "assists"))).thenReturn(List.of(new StatisticEntryGenerateDto("goals", "2", true)));
        when(photoPipelineService.prepareManualPhoto(any(), any(), any())).thenReturn(manualPhoto());

        draftReviewService.usePhotoUrl(7, "https://photos.example/p.jpg", null);

        assertEquals(List.of("goals", "assists"), post.getFeaturedStats());
        verify(postRepository).save(post);
    }

    @Test
    public void usePhotoUrl_fetchFails_nothingSaved() throws Exception {
        when(imageGeneratorProperties.isEnabled()).thenReturn(true);
        post.setFeaturedStats(List.of("goals"));
        when(postDraftService.selectedStats(any(), anyList())).thenReturn(List.of(new StatisticEntryGenerateDto("goals", "2", true)));
        when(photoPipelineService.prepareManualPhoto(any(), any(), any())).thenThrow(new ImageFetchException("Image URL must use http or https"));

        assertThrows(ImageFetchException.class, () -> draftReviewService.usePhotoUrl(7, "file:///etc/passwd", null));
        verify(postDraftService, never()).renderStandout(any(), anyList(), any());
        verify(postRepository, never()).save(any());
    }

    @Test
    public void usePhotoUrl_notPending_throws() throws Exception {
        when(imageGeneratorProperties.isEnabled()).thenReturn(true);
        post.setDraftStatus(DraftStatus.APPROVED);

        assertThrows(IllegalStateException.class, () -> draftReviewService.usePhotoUrl(7, "https://photos.example/p.jpg", null));
        verify(photoPipelineService, never()).prepareManualPhoto(any(), any(), any());
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
