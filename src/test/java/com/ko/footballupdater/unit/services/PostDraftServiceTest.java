package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.CaptionAgent;
import com.ko.footballupdater.llm.EditorAgent;
import com.ko.footballupdater.llm.LlmClient;
import com.ko.footballupdater.llm.VisionQaService;
import com.ko.footballupdater.photo.PhotoCandidate;
import com.ko.footballupdater.photo.PhotoPipelineService;
import com.ko.footballupdater.photo.MatchResult;
import com.ko.footballupdater.models.form.ImageGenParams;
import java.awt.image.BufferedImage;
import java.util.Optional;
import com.ko.footballupdater.llm.models.EditorPostType;
import com.ko.footballupdater.llm.models.EditorResult;
import com.ko.footballupdater.llm.models.ValidatedEditorDecision;
import com.ko.footballupdater.llm.models.ValidatedSummaryGroup;
import com.ko.footballupdater.models.CaptionSource;
import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.services.AmazonS3Service;
import com.ko.footballupdater.services.ImageGeneratorService;
import com.ko.footballupdater.services.PostDraftService;
import com.ko.footballupdater.unit.llm.LlmTestData;
import com.ko.footballupdater.utils.PostHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostDraftServiceTest {

    @InjectMocks
    private PostDraftService postDraftService;

    @Mock
    private EditorAgent editorAgent;
    @Mock
    private PhotoPipelineService photoPipelineService;
    @Mock
    private VisionQaService visionQaService;
    @Mock
    private CaptionAgent captionAgent;
    @Mock
    private LlmClient llmClient;
    @Mock
    private LlmProperties llmProperties;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ImageGeneratorService imageGeneratorService;
    @Mock
    private AmazonS3Service amazonS3Service;
    @Mock
    private ImageGeneratorProperties imageGeneratorProperties;
    @Mock
    private InstagramPostProperies instagramPostProperies;
    @Mock
    private PostHelper postHelper;

    private Post post1;
    private Post post2;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(llmProperties.getMaxPostsPerRun()).thenReturn(10);
        when(imageGeneratorProperties.isEnabled()).thenReturn(true);
        when(llmClient.isEnabled()).thenReturn(true);
        when(captionAgent.loadKnownNames()).thenReturn(Set.of("Jamie Rivers"));
        when(postHelper.generateCaptionHashtagSuffix(any(), any())).thenReturn("\n\n#tags");
        when(postRepository.findByPostedStatusOrderByDateGeneratedDesc(eq(false), any())).thenReturn(List.of());
        when(photoPipelineService.findAutomaticPhoto(any())).thenReturn(Optional.empty());
        post1 = LlmTestData.post(1, "Jamie Rivers");
        post1.setImagesFileNames(new ArrayList<>(List.of("stats1.jpg")));
        post2 = LlmTestData.post(2, "Casey Moor");
    }

    private void givenDecisions(ValidatedEditorDecision... decisions) {
        Map<Integer, ValidatedEditorDecision> map = new java.util.HashMap<>();
        for (ValidatedEditorDecision decision : decisions) {
            map.put(decision.postId(), decision);
        }
        when(editorAgent.decide(anyList(), anyList())).thenReturn(new EditorResult(map, List.of()));
    }

    @Test
    public void processNewPosts_standoutPick_generatesStandoutImageAndLlmCaption() throws Exception {
        givenDecisions(
                new ValidatedEditorDecision(1, true, EditorPostType.STANDOUT, List.of("goals", "assists"), "Two goals", true),
                new ValidatedEditorDecision(2, true, EditorPostType.ALL_STAT, List.of(), "Solid", true));
        when(captionAgent.generateCaption(eq(post1), anyList(), anyString(), any())).thenReturn(new CaptionAgent.CaptionResult("llm caption", CaptionSource.LLM));
        when(captionAgent.generateCaption(eq(post2), anyList(), anyString(), any())).thenReturn(new CaptionAgent.CaptionResult("template caption", CaptionSource.TEMPLATE));

        postDraftService.processNewPosts(List.of(post1, post2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StatisticEntryGenerateDto>> statsCaptor = ArgumentCaptor.forClass(List.class);
        verify(imageGeneratorService).generateStandoutStatsImage(eq(post1), statsCaptor.capture(), isNull(), eq(true));
        assertEquals("goals", statsCaptor.getValue().get(0).getName());
        assertEquals("2", statsCaptor.getValue().get(0).getValue());
        // Singular label when the value is 1
        assertEquals("assist", statsCaptor.getValue().get(1).getName());
        verify(amazonS3Service).uploadToS3(post1, true);
        // The already uploaded stat images aren't uploaded again
        assertTrue(post1.getImagesFileNames().isEmpty());

        assertEquals(PostType.STANDOUT_STATS_POST, post1.getPostType());
        assertEquals(DraftStatus.PENDING_REVIEW, post1.getDraftStatus());
        assertEquals("llm caption", post1.getCaption());
        assertEquals(CaptionSource.LLM, post1.getCaptionSource());
        assertEquals(List.of("goals", "assists"), post1.getFeaturedStats());
        assertEquals("Two goals", post1.getLlmReason());

        assertEquals(PostType.ALL_STAT_POST, post2.getPostType());
        assertEquals(CaptionSource.TEMPLATE, post2.getCaptionSource());
        verify(postRepository).save(post1);
        verify(postRepository).save(post2);
    }

    @Test
    public void processNewPosts_excludedPost_rejectedWithoutCaptionOrImage() throws Exception {
        givenDecisions(new ValidatedEditorDecision(1, false, EditorPostType.ALL_STAT, List.of(), "Only 5 minutes", true));

        postDraftService.processNewPosts(List.of(post1));

        assertEquals(DraftStatus.REJECTED, post1.getDraftStatus());
        assertTrue(post1.getLlmReason().startsWith("Skipped by editor"));
        verify(captionAgent, never()).generateCaption(any(), anyList(), anyString(), any());
        verify(imageGeneratorService, never()).generateStandoutStatsImage(any(), anyList(), any(), anyBoolean());
        verify(postRepository).save(post1);
    }

    @Test
    public void processNewPosts_standoutImageFails_keepsAllStatTypeAndNotesReason() throws Exception {
        givenDecisions(new ValidatedEditorDecision(1, true, EditorPostType.STANDOUT, List.of("goals"), "Two goals", true));
        doThrow(new Exception("missing base images")).when(imageGeneratorService).generateStandoutStatsImage(any(), anyList(), any(), eq(true));
        when(captionAgent.generateCaption(any(), anyList(), anyString(), any())).thenReturn(new CaptionAgent.CaptionResult("template caption", CaptionSource.TEMPLATE));

        postDraftService.processNewPosts(List.of(post1));

        assertEquals(PostType.ALL_STAT_POST, post1.getPostType());
        assertTrue(post1.getLlmReason().contains("standout image not generated"));
        assertEquals(DraftStatus.PENDING_REVIEW, post1.getDraftStatus());
        verify(postRepository).save(post1);
    }

    @Test
    public void processNewPosts_playerStandoutBaseMissing_genericBaseUsedAndNoted() throws Exception {
        givenDecisions(new ValidatedEditorDecision(1, true, EditorPostType.STANDOUT, List.of("goals"), "Two goals", true));
        when(imageGeneratorService.generateStandoutStatsImage(eq(post1), anyList(), isNull(), eq(true))).thenReturn(true);
        when(captionAgent.generateCaption(any(), anyList(), anyString(), any())).thenReturn(new CaptionAgent.CaptionResult("c", CaptionSource.TEMPLATE));

        postDraftService.processNewPosts(List.of(post1));

        assertEquals(PostType.STANDOUT_STATS_POST, post1.getPostType());
        assertTrue(post1.getLlmReason().contains("generic base image"));
        verify(amazonS3Service).uploadToS3(post1, true);
    }

    @Test
    public void processNewPosts_llmEnabled_runsVisionQaBeforeSaving() throws Exception {
        givenDecisions(new ValidatedEditorDecision(1, true, EditorPostType.ALL_STAT, List.of(), "Solid", true));
        when(captionAgent.generateCaption(any(), anyList(), anyString(), any())).thenReturn(new CaptionAgent.CaptionResult("c", CaptionSource.LLM));

        postDraftService.processNewPosts(List.of(post1));

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(captionAgent, visionQaService, postRepository);
        inOrder.verify(captionAgent).generateCaption(any(), anyList(), anyString(), any());
        inOrder.verify(visionQaService).review(post1);
        inOrder.verify(postRepository).save(post1);
    }

    @Test
    public void processNewPosts_automaticPhotoFound_standoutRenderedOnPhoto() throws Exception {
        givenDecisions(new ValidatedEditorDecision(1, true, EditorPostType.STANDOUT, List.of("goals"), "Two goals", true));
        BufferedImage photo = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageGenParams params = new ImageGenParams();
        when(photoPipelineService.findAutomaticPhoto(post1)).thenReturn(Optional.of(new PhotoPipelineService.SelectedPhoto(
                new PhotoCandidate("https://photos.example/p.jpg", "SOME_SOURCE", "CC BY 4.0", null, null), photo, params,
                MatchResult.pass(0.9, "match"))));
        when(captionAgent.generateCaption(any(), anyList(), anyString(), any())).thenReturn(new CaptionAgent.CaptionResult("c", CaptionSource.TEMPLATE));

        postDraftService.processNewPosts(List.of(post1));

        verify(imageGeneratorService).generateStandoutStatsImage(eq(post1), anyList(), eq(params), eq(false), eq(photo));
        verify(imageGeneratorService, never()).generateStandoutStatsImage(any(), anyList(), isNull(), eq(true));
        assertEquals(PostType.STANDOUT_STATS_POST, post1.getPostType());
        assertEquals("https://photos.example/p.jpg", post1.getPhotoSourceUrl());
        assertEquals("SOME_SOURCE", post1.getPhotoSourceName());
        assertEquals("CC BY 4.0", post1.getPhotoLicence());
        assertTrue(post1.getLlmReason().contains("uses a photo from SOME_SOURCE"));
    }

    @Test
    public void processNewPosts_llmDisabled_noCaptionCallsTemplateKept() throws Exception {
        when(llmClient.isEnabled()).thenReturn(false);
        givenDecisions(new ValidatedEditorDecision(1, true, EditorPostType.ALL_STAT, List.of(), "Rule-based: no standout stats", false));

        postDraftService.processNewPosts(List.of(post1));

        verify(captionAgent, never()).generateCaption(any(), anyList(), anyString(), any());
        verify(visionQaService, never()).review(any());
        assertEquals("template caption", post1.getCaption());
        assertEquals(CaptionSource.TEMPLATE, post1.getCaptionSource());
        assertEquals(DraftStatus.PENDING_REVIEW, post1.getDraftStatus());
    }

    @Test
    public void processNewPosts_summaryGroup_notedInReason() {
        Map<Integer, ValidatedEditorDecision> map = new java.util.HashMap<>();
        map.put(1, new ValidatedEditorDecision(1, true, EditorPostType.SUMMARY_MEMBER, List.of(), "Fits a summary", true));
        when(editorAgent.decide(anyList(), anyList())).thenReturn(new EditorResult(map,
                List.of(new ValidatedSummaryGroup("Northfield", List.of(1, 3), "Same team"))));
        when(captionAgent.generateCaption(any(), anyList(), anyString(), any())).thenReturn(new CaptionAgent.CaptionResult("c", CaptionSource.TEMPLATE));

        postDraftService.processNewPosts(List.of(post1));

        assertTrue(post1.getLlmReason().contains("suggested for a summary post"));
        assertTrue(post1.getLlmReason().contains("Summary group suggestion 'Northfield': posts 1, 3"));
    }
}
