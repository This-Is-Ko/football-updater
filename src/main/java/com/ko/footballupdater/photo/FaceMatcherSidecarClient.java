package com.ko.footballupdater.photo;

import com.ko.footballupdater.configuration.FaceMatchingProperties;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerFaceEmbedding;
import com.ko.footballupdater.photo.face.FaceSidecarClient;
import com.ko.footballupdater.photo.face.FaceSidecarException;
import com.ko.footballupdater.photo.face.FaceSidecarModels.MatchResponse;
import com.ko.footballupdater.photo.face.FaceSidecarModels.ScoredFace;
import com.ko.footballupdater.photo.face.IdentityCrossCheck;
import com.ko.footballupdater.repositories.PlayerFaceEmbeddingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Matches the photo's main face with the player's enrolled reference faces through the face sidecar,
 * then cross-checks the jersey number, shirt name and caption with Claude.
 * <p>
 * Automatic photos fail safe: if the sidecar is down or slow, the player isn't enrolled, no face is found,
 * the face is too different, or the cross-check contradicts the match, the photo is skipped.
 * A photo the reviewer picked always passes, but with a low confidence when it couldn't be verified,
 * so it is flagged on the approval card.
 */
@Slf4j
@Component
public class FaceMatcherSidecarClient implements PlayerMatcher {

    public static final String NAME = "FACE_SIDECAR";

    @Autowired
    private FaceMatchingProperties properties;

    @Autowired
    private FaceSidecarClient sidecarClient;

    @Autowired
    private PlayerFaceEmbeddingRepository embeddingRepository;

    @Autowired
    private IdentityCrossCheck identityCrossCheck;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public MatchResult match(Player player, PhotoCandidate candidate, BufferedImage image) {
        boolean manual = candidate != null && ManualUrlPhotoSource.NAME.equals(candidate.sourceName());
        if (player == null || player.getId() == null || image == null) {
            return manual ? unverified("No player or image to check") : MatchResult.fail("No player or image to check");
        }

        List<float[]> references = embeddingRepository.findByPlayerIdAndModel(player.getId(), properties.getModel()).stream()
                .map(PlayerFaceEmbedding::embeddingValues).toList();
        if (references.isEmpty() || (!manual && references.size() < properties.getMinReferences())) {
            String reason = player.getName() + " has " + references.size() + " reference photos, needs " + properties.getMinReferences();
            return manual ? unverified(reason + ", face not checked") : MatchResult.fail(reason);
        }

        MatchResponse response;
        try {
            response = sidecarClient.match(VisionImages.toJpeg(image).data(), references);
        } catch (FaceSidecarException ex) {
            log.atWarn().setMessage("Face sidecar unavailable, " + (manual ? "reviewer photo not verified" : "skipping photo"))
                    .setCause(ex).addKeyValue("playerId", player.getId()).log();
            return manual ? unverified("Face matcher unavailable, face not checked") : MatchResult.fail("Face matcher unavailable, photo skipped");
        } catch (Exception ex) {
            log.atWarn().setMessage("Unable to prepare photo for face matching").setCause(ex).addKeyValue("playerId", player.getId()).log();
            return manual ? unverified("Face matching failed, face not checked") : MatchResult.fail("Face matching failed");
        }
        if (response.model() != null && !response.model().equals(properties.getModel())) {
            String reason = "Face sidecar runs model " + response.model() + " but references are " + properties.getModel();
            return manual ? unverified(reason) : MatchResult.fail(reason);
        }

        ScoredFace subject = response.subject();
        if (subject == null) {
            return manual ? unverified("No face found") : MatchResult.fail("No face found");
        }
        List<String> reasons = new ArrayList<>();
        double similarity = subject.maxSimilarity();
        reasons.add(String.format("Face similarity %.2f (%d reference photos)", similarity, references.size()));
        if (response.faceCount() > 1) {
            reasons.add(response.faceCount() + " faces in the photo, matched the largest");
        }
        ScoredFace best = response.bestMatch();
        boolean playerIsSomeoneElseInPhoto = best != null && best.index() != subject.index()
                && best.maxSimilarity() >= properties.getMinSimilarity() && best.maxSimilarity() > similarity;
        if (playerIsSomeoneElseInPhoto) {
            String reason = player.getName() + " seems to be in the photo but isn't the main subject";
            if (!manual) {
                return MatchResult.fail(reason);
            }
            reasons.add(reason);
        }
        if (!manual && similarity < properties.getMinSimilarity()) {
            return new MatchResult(clamp(similarity), false, reasons);
        }

        double confidence = clamp(similarity);
        if (playerIsSomeoneElseInPhoto) {
            confidence = 0;
        }
        if (properties.isCrossCheckEnabled()) {
            IdentityCrossCheck.Result crossCheck = identityCrossCheck.check(player, candidate, image);
            reasons.addAll(crossCheck.reasons());
            if (crossCheck.verdict() == IdentityCrossCheck.Verdict.CONTRADICTS) {
                if (!manual) {
                    return new MatchResult(confidence, false, reasons);
                }
                confidence = 0;
            }
        }
        return new MatchResult(confidence, true, reasons);
    }

    private static MatchResult unverified(String reason) {
        return new MatchResult(0, true, List.of("Photo chosen by the reviewer", reason));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
