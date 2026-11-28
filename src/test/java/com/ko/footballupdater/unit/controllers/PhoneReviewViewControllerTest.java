package com.ko.footballupdater.unit.controllers;

import com.ko.footballupdater.controllers.views.PhoneReviewViewController;
import com.ko.footballupdater.interceptors.SecurityConfig;
import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.services.DraftReviewService;
import com.ko.footballupdater.services.ReviewLinkService;
import com.ko.footballupdater.unit.llm.LlmTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhoneReviewViewController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"dashboard.password=test-password"})
public class PhoneReviewViewControllerTest {

    private static final String TOKEN = "7.123.nonce.sig";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewLinkService reviewLinkService;

    @MockBean
    private DraftReviewService draftReviewService;

    private Post post;

    @BeforeEach
    public void setup() {
        post = LlmTestData.post(7, "Jamie Rivers");
        post.setDraftStatus(DraftStatus.PENDING_REVIEW);
        post.setImagesUrls(List.of("https://s3/standout.jpg"));
        when(reviewLinkService.resolve(TOKEN)).thenReturn(Optional.of(post));
        when(reviewLinkService.consume(TOKEN)).thenReturn(Optional.of(post));
    }

    @Test
    public void openLink_withoutLogin_showsDraftAndDoesNotApprove() throws Exception {
        mockMvc.perform(get("/review/" + TOKEN).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Jamie Rivers")))
                .andExpect(content().string(containsString("Approve and post")));
        verify(reviewLinkService, never()).consume(anyString());
        verify(draftReviewService, never()).approve(anyInt(), any());
    }

    @Test
    public void openInvalidLink_showsExpiredMessage() throws Exception {
        mockMvc.perform(get("/review/bad").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("expired or was already used")))
                .andExpect(content().string(not(containsString("Approve and post"))));
    }

    @Test
    public void approve_withCsrf_consumesTokenAndApprovesWithCaption() throws Exception {
        mockMvc.perform(post("/review/" + TOKEN + "/approve").with(csrf()).param("caption", "Edited"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Approved and posted")));
        verify(reviewLinkService).consume(TOKEN);
        verify(draftReviewService).approve(7, "Edited");
    }

    @Test
    public void approve_withoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/review/" + TOKEN + "/approve").param("caption", "Edited"))
                .andExpect(status().isForbidden());
        verify(draftReviewService, never()).approve(anyInt(), any());
    }

    @Test
    public void approve_usedToken_doesNothing() throws Exception {
        when(reviewLinkService.consume("used")).thenReturn(Optional.empty());
        mockMvc.perform(post("/review/used/approve").with(csrf()))
                .andExpect(content().string(containsString("expired or was already used")));
        verify(draftReviewService, never()).approve(anyInt(), any());
    }

    @Test
    public void approve_failure_restoresTokenForRetry() throws Exception {
        doThrow(new IllegalStateException("Instagram logged out")).when(draftReviewService).approve(eq(7), any());
        mockMvc.perform(post("/review/" + TOKEN + "/approve").with(csrf()))
                .andExpect(content().string(containsString("Instagram logged out")))
                .andExpect(content().string(containsString("Try again")));
        verify(reviewLinkService).restore(post, TOKEN);
    }

    @Test
    public void reject_withCsrf_rejects() throws Exception {
        mockMvc.perform(post("/review/" + TOKEN + "/reject").with(csrf()))
                .andExpect(content().string(containsString("Rejected")));
        verify(draftReviewService).reject(7);
    }
}
