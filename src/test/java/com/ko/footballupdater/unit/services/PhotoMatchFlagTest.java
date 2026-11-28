package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.photo.MatchResult;
import com.ko.footballupdater.services.PostDraftService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhotoMatchFlagTest {

    private final PostDraftService service = new PostDraftService();

    @Test
    public void lowConfidence_flaggedWithNotes() {
        Post post = new Post();
        service.applyMatchResult(post, new MatchResult(0.38, true, List.of("Face similarity 0.38", "two\nlines")));

        assertTrue(post.getPhotoMatchFlagged());
        assertEquals(0.38, post.getPhotoMatchConfidence());
        assertEquals(List.of("Face similarity 0.38", "two lines"), post.getPhotoMatchNoteList());
    }

    @Test
    public void highConfidence_notFlagged() {
        Post post = new Post();
        service.applyMatchResult(post, MatchResult.pass(1.0, "Photo chosen by the reviewer"));

        assertFalse(post.getPhotoMatchFlagged());
    }

    @Test
    public void noResult_clearsFields() {
        Post post = new Post();
        post.setPhotoMatchFlagged(true);
        service.applyMatchResult(post, null);

        assertNull(post.getPhotoMatchFlagged());
        assertTrue(post.getPhotoMatchNoteList().isEmpty());
    }
}
