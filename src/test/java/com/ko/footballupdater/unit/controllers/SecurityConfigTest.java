package com.ko.footballupdater.unit.controllers;

import com.ko.footballupdater.controllers.FacebookApiController;
import com.ko.footballupdater.controllers.PlayerController;
import com.ko.footballupdater.controllers.TiktokApiController;
import com.ko.footballupdater.controllers.views.PerformanceAnalysisViewController;
import com.ko.footballupdater.controllers.views.PlayerViewController;
import com.ko.footballupdater.controllers.views.PostViewController;
import com.ko.footballupdater.controllers.views.ReviewViewController;
import com.ko.footballupdater.controllers.views.TeamViewController;
import com.ko.footballupdater.interceptors.SecurityConfig;
import com.ko.footballupdater.interceptors.ValidateHeaderSecretInterceptor;
import com.ko.footballupdater.interceptors.WebConfig;
import com.ko.footballupdater.models.form.FacebookApiDto;
import com.ko.footballupdater.models.form.TiktokApiDto;
import com.ko.footballupdater.services.DraftReviewService;
import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.PerformanceAnalysisService;
import com.ko.footballupdater.services.PlayerViewService;
import com.ko.footballupdater.services.TeamViewService;
import com.ko.footballupdater.services.PlayerService;
import com.ko.footballupdater.services.PostService;
import com.ko.footballupdater.services.TiktokApiService;
import com.ko.footballupdater.utils.PostHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.ko.footballupdater.photo.face.FaceEnrolmentService;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PostViewController.class, ReviewViewController.class, FacebookApiController.class,
        TiktokApiController.class, PlayerController.class, TeamViewController.class, PlayerViewController.class,
        PerformanceAnalysisViewController.class})
@Import({SecurityConfig.class, WebConfig.class, ValidateHeaderSecretInterceptor.class})
@TestPropertySource(properties = {"dashboard.password=test-password", "endpoint.secret=test-secret"})
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;
    @MockBean
    private PlayerService playerService;
    @MockBean
    private FacebookApiService facebookApiService;
    @MockBean
    private TiktokApiService tiktokApiService;
    @MockBean
    private PostHelper postHelper;
    @MockBean
    private DraftReviewService draftReviewService;
    @MockBean
    private TeamViewService teamViewService;
    @MockBean
    private PlayerViewService playerViewService;
    @MockBean
    private PerformanceAnalysisService performanceAnalysisService;
    @MockBean
    private FaceEnrolmentService faceEnrolmentService;

    private static MockHttpServletRequestBuilder browser(MockHttpServletRequestBuilder builder) {
        return builder.accept(MediaType.TEXT_HTML);
    }

    @BeforeEach
    public void setup() {
        when(postService.getPosts(any(), any(), any())).thenReturn(List.of());
        when(facebookApiService.prepareFacebookApiDto()).thenReturn(new FacebookApiDto());
        when(tiktokApiService.prepareTiktokApiDto()).thenReturn(new TiktokApiDto());
        when(draftReviewService.getPendingDrafts()).thenReturn(List.of());
        when(draftReviewService.getRejectedDrafts()).thenReturn(List.of());
    }

    // --- Unauthenticated access to /posts/** redirects to the login page

    @Test
    public void posts_notLoggedIn_redirectsToLogin() throws Exception {
        mockMvc.perform(browser(get("/posts"))).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(get("/posts/review"))).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(get("/posts/prepare/summary"))).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(get("/posts/prepare-standout-image")).param("postId", "1")).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(get("/posts/prepare-upload")).param("postId", "1")).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(get("/posts/check-for-new"))).andExpect(redirectedUrlPattern("**/login"));
        verify(playerService, never()).updateDataForAllPlayers();
    }

    @Test
    public void posts_notLoggedInNonBrowserClient_unauthorized() throws Exception {
        mockMvc.perform(post("/posts/delete").with(csrf()).param("postId", "1")).andExpect(status().isUnauthorized());
        verify(postService, never()).deletePost(anyInt());
    }

    @Test
    public void postsDelete_notLoggedIn_redirectsWithoutDeleting() throws Exception {
        mockMvc.perform(browser(post("/posts/delete")).with(csrf()).param("postId", "1")).andExpect(redirectedUrlPattern("**/login"));
        verify(postService, never()).deletePost(anyInt());
    }

    @Test
    @WithMockUser
    public void postsDelete_loggedInPostWithCsrf_deletes() throws Exception {
        mockMvc.perform(browser(post("/posts/delete")).with(csrf()).param("postId", "4")).andExpect(redirectedUrl("/posts"));
        verify(postService).deletePost(4);
    }

    @Test
    @WithMockUser
    public void postsDelete_loggedInWithoutCsrf_forbidden() throws Exception {
        mockMvc.perform(browser(post("/posts/delete")).param("postId", "4")).andExpect(status().isForbidden());
        verify(postService, never()).deletePost(anyInt());
    }

    @Test
    @WithMockUser
    public void postsDelete_getNoLongerDeletes() throws Exception {
        mockMvc.perform(browser(get("/posts/delete")).param("postId", "4")).andExpect(status().isMethodNotAllowed());
        verify(postService, never()).deletePost(anyInt());
    }

    @Test
    public void teamsPlayersPerformance_notLoggedIn_redirectToLogin() throws Exception {
        mockMvc.perform(browser(get("/teams"))).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(get("/players"))).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(get("/performance/recent-highlights"))).andExpect(redirectedUrlPattern("**/login"));
        verify(teamViewService, never()).getTeams();
        verify(playerViewService, never()).getPlayers();
    }

    @Test
    public void postsUpload_notLoggedIn_redirectsWithoutUploading() throws Exception {
        mockMvc.perform(browser(post("/posts/upload")).with(csrf()).param("postId", "1"))
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(post("/posts/generate")).with(csrf()).param("postId", "1"))
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(post("/posts/generate/summary")).with(csrf()))
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(browser(post("/posts/review/1/approve")).with(csrf()).param("caption", "c"))
                .andExpect(redirectedUrlPattern("**/login"));
        verify(postService, never()).uploadPost(any());
        verify(postService, never()).generateStandoutPost(any());
        verify(postService, never()).generateSummaryPost(any());
        verify(draftReviewService, never()).approve(anyInt(), anyString());
    }

    // --- Logged in

    @Test
    @WithMockUser
    public void posts_loggedIn_ok() throws Exception {
        mockMvc.perform(browser(get("/posts"))).andExpect(status().isOk());
        mockMvc.perform(browser(get("/posts/review"))).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void reviewReject_loggedInWithCsrf_rejectsDraft() throws Exception {
        mockMvc.perform(browser(post("/posts/review/5/reject")).with(csrf()))
                .andExpect(redirectedUrl("/posts/review"));
        verify(draftReviewService).reject(5);
    }

    @Test
    public void reviewPhoto_notLoggedIn_redirectsWithoutFetching() throws Exception {
        mockMvc.perform(browser(post("/posts/review/5/photo")).with(csrf()).param("photoUrl", "https://photos.example/p.jpg"))
                .andExpect(redirectedUrlPattern("**/login"));
        verify(draftReviewService, never()).usePhotoUrl(anyInt(), anyString(), any());
    }

    @Test
    @WithMockUser
    public void reviewPhoto_loggedInWithCsrf_usesPhoto() throws Exception {
        mockMvc.perform(browser(post("/posts/review/5/photo")).with(csrf())
                        .param("photoUrl", "https://photos.example/p.jpg").param("licence", "CC BY"))
                .andExpect(redirectedUrl("/posts/review"));
        verify(draftReviewService).usePhotoUrl(5, "https://photos.example/p.jpg", "CC BY");
    }

    @Test
    @WithMockUser
    public void reviewPhoto_loggedInWithoutCsrf_forbidden() throws Exception {
        mockMvc.perform(browser(post("/posts/review/5/photo")).param("photoUrl", "https://photos.example/p.jpg"))
                .andExpect(status().isForbidden());
        verify(draftReviewService, never()).usePhotoUrl(anyInt(), anyString(), any());
    }

    @Test
    @WithMockUser
    public void reviewApprove_loggedInWithoutCsrf_forbidden() throws Exception {
        mockMvc.perform(browser(post("/posts/review/5/approve")).param("caption", "c")).andExpect(status().isForbidden());
        verify(draftReviewService, never()).approve(anyInt(), anyString());
    }

    @Test
    public void login_thenRedirectedBackToRequestedPostsPage() throws Exception {
        MvcResult unauthenticated = mockMvc.perform(browser(get("/posts/review"))).andExpect(redirectedUrlPattern("**/login")).andReturn();
        MockHttpSession session = (MockHttpSession) unauthenticated.getRequest().getSession();

        mockMvc.perform(post("/login").param("username", "admin").param("password", "test-password").with(csrf()).session(session))
                .andExpect(redirectedUrlPattern("**/posts/review*"));
    }

    @Test
    public void login_wrongPassword_rejected() throws Exception {
        mockMvc.perform(formLogin().user("admin").password("wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }

    // --- OAuth callbacks stay public and land on /posts

    @Test
    @WithMockUser
    public void facebookAuthCallback_loggedIn_handlesLoginAndRedirectsToPosts() throws Exception {
        mockMvc.perform(get("/facebook/auth").param("state", "s").param("code", "c"))
                .andExpect(redirectedUrl("/posts"));
        verify(facebookApiService).handleLogin("s", "c");
        // The redirect target loads for the logged in user
        mockMvc.perform(browser(get("/posts"))).andExpect(status().isOk());
    }

    @Test
    public void oauthCallbacks_notLoggedIn_stillHandled() throws Exception {
        mockMvc.perform(get("/facebook/auth").param("state", "s").param("code", "c"))
                .andExpect(redirectedUrl("/posts"));
        verify(facebookApiService).handleLogin("s", "c");
        mockMvc.perform(get("/tiktok/auth").param("state", "s").param("code", "c"))
                .andExpect(redirectedUrl("/posts"));
        verify(tiktokApiService).handleLogin("s", "c");
    }

    // --- Static assets and the API

    @Test
    public void staticAssets_notLoggedIn_served() throws Exception {
        mockMvc.perform(get("/styles/styles.css")).andExpect(status().isOk());
        mockMvc.perform(get("/images/favicon.ico")).andExpect(status().isOk());
        mockMvc.perform(get("/terms-and-privacy.html")).andExpect(status().isOk());
    }

    @Test
    public void api_withAuthSecretHeader_noLoginNeeded() throws Exception {
        when(playerService.getPlayers()).thenReturn(List.of());
        mockMvc.perform(get("/api/player/all").header("Auth-Secret", "test-secret")).andExpect(status().isOk());
    }

    @Test
    public void api_withoutAuthSecretHeader_rejectedByInterceptor() {
        Exception ex = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> mockMvc.perform(get("/api/player/all")));
        assertTrue(ex.getMessage().contains("Secret doesn't match"));
        verify(playerService, never()).getPlayers();
    }
}
