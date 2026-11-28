package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.ReviewLinkProperties;
import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.repositories.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and checks the phone review links. A token is {@code postId.expiry.nonce.signature}, signed with
 * HMAC-SHA256. It is single-use: the nonce is stored on the post and cleared when the link is used, and a
 * link only opens a draft that is still pending review.
 */
@Slf4j
@Service
public class ReviewLinkService {

    private static final String HMAC = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private ReviewLinkProperties reviewLinkProperties;

    @Autowired
    private PostRepository postRepository;

    private Clock clock = Clock.systemUTC();

    public boolean isEnabled() {
        return reviewLinkProperties.isEnabled();
    }

    /**
     * Sets a new nonce on the post (replacing any earlier link) and returns the link. The caller saves the post.
     */
    public String issueLink(Post post) {
        if (!isEnabled() || post.getId() == null) {
            return null;
        }
        byte[] nonceBytes = new byte[18];
        random.nextBytes(nonceBytes);
        String nonce = ENCODER.encodeToString(nonceBytes);
        post.setReviewLinkNonce(nonce);
        long expiry = clock.instant().plus(Duration.ofHours(reviewLinkProperties.getExpiryHours())).getEpochSecond();
        String payload = post.getId() + "." + expiry + "." + nonce;
        String token = payload + "." + sign(payload);
        return trimTrailingSlash(reviewLinkProperties.getBaseUrl()) + "/review/" + token;
    }

    public String reviewQueueUrl() {
        return isEnabled() ? trimTrailingSlash(reviewLinkProperties.getBaseUrl()) + "/posts/review" : null;
    }

    /**
     * @return the post the token is for, when the signature is valid, it hasn't expired or been used, and the
     * post is still pending review
     */
    public Optional<Post> resolve(String token) {
        if (!isEnabled() || token == null) {
            return Optional.empty();
        }
        String[] parts = token.split("[.]");
        if (parts.length != 4) {
            return Optional.empty();
        }
        String payload = parts[0] + "." + parts[1] + "." + parts[2];
        byte[] expected = sign(payload).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, parts[3].getBytes(StandardCharsets.US_ASCII))) {
            log.atWarn().setMessage("Review link with an invalid signature").log();
            return Optional.empty();
        }
        int postId;
        long expiry;
        try {
            postId = Integer.parseInt(parts[0]);
            expiry = Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        if (clock.instant().getEpochSecond() > expiry) {
            return Optional.empty();
        }
        return postRepository.findById(postId)
                .filter(post -> DraftStatus.PENDING_REVIEW.equals(post.getDraftStatus()))
                .filter(post -> !post.isPostedStatus())
                .filter(post -> post.getReviewLinkNonce() != null
                        && MessageDigest.isEqual(post.getReviewLinkNonce().getBytes(StandardCharsets.US_ASCII),
                        parts[2].getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * Uses up the token: returns the post and clears its nonce, so a second use (or a double tap) finds nothing.
     */
    public synchronized Optional<Post> consume(String token) {
        Optional<Post> post = resolve(token);
        post.ifPresent(p -> {
            p.setReviewLinkNonce(null);
            postRepository.save(p);
        });
        return post;
    }

    /**
     * Puts a consumed token back, used when approving failed so the link can be retried.
     */
    public void restore(Post post, String token) {
        String[] parts = token.split("[.]");
        if (parts.length == 4) {
            Post current = postRepository.findById(post.getId()).orElse(post);
            if (DraftStatus.PENDING_REVIEW.equals(current.getDraftStatus()) && !current.isPostedStatus()) {
                current.setReviewLinkNonce(parts[2]);
                postRepository.save(current);
            }
        }
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(reviewLinkProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC));
            return ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign review link", ex);
        }
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
