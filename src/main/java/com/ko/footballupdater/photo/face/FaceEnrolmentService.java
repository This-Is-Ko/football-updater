package com.ko.footballupdater.photo.face;

import com.ko.footballupdater.configuration.FaceMatchingProperties;
import com.ko.footballupdater.models.PlayerFaceEmbedding;
import com.ko.footballupdater.photo.ImageFetchException;
import com.ko.footballupdater.photo.ImageFetcher;
import com.ko.footballupdater.photo.VisionImages;
import com.ko.footballupdater.photo.face.FaceSidecarModels.EmbedResponse;
import com.ko.footballupdater.photo.face.FaceSidecarModels.EmbeddedFace;
import com.ko.footballupdater.repositories.PlayerFaceEmbeddingRepository;
import com.ko.footballupdater.repositories.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Enrols a player's reference photos: each photo is sent to the sidecar and its face embedding is stored in MySQL.
 */
@Slf4j
@Service
public class FaceEnrolmentService {

    /**
     * When a reference photo has several faces, the largest is used only if it is this many times bigger than the next.
     */
    static final double DOMINANT_FACE_AREA_RATIO = 4.0;

    public record RejectedPhoto(String url, String reason) {
    }

    public record EnrolmentResult(int playerId, int enrolled, int minReferences, List<RejectedPhoto> rejected) {
        public boolean readyForMatching() {
            return enrolled >= minReferences;
        }
    }

    private record Accepted(String url, EmbeddedFace face) {
    }

    @Autowired
    private FaceSidecarClient sidecarClient;

    @Autowired
    private FaceMatchingProperties properties;

    @Autowired
    private PlayerFaceEmbeddingRepository embeddingRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ImageFetcher imageFetcher;

    /**
     * Replace the player's reference embeddings with the faces from these photos (at most photo.face.max-references).
     * Existing embeddings are only removed when at least one new photo is accepted.
     *
     * @throws IllegalArgumentException if the player doesn't exist or no URLs are given
     * @throws FaceSidecarException     if the sidecar can't be reached, in which case nothing is changed
     */
    @Transactional
    public EnrolmentResult enrol(Integer playerId, List<String> urls) throws FaceSidecarException {
        if (playerId == null || playerRepository.findById(playerId).isEmpty()) {
            throw new IllegalArgumentException("Player can't be found");
        }
        List<String> distinctUrls = urls == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(urls.stream()
                .filter(url -> url != null && !url.isBlank()).map(String::trim).toList()));
        if (distinctUrls.isEmpty()) {
            throw new IllegalArgumentException("Give at least one reference photo URL");
        }
        List<RejectedPhoto> rejected = new ArrayList<>();
        List<Accepted> accepted = new ArrayList<>();
        for (String url : distinctUrls) {
            if (accepted.size() >= properties.getMaxReferences()) {
                rejected.add(new RejectedPhoto(url, "More than " + properties.getMaxReferences() + " reference photos"));
                continue;
            }
            byte[] jpeg;
            try {
                BufferedImage image = imageFetcher.fetchImage(url);
                jpeg = VisionImages.toJpeg(image).data();
            } catch (ImageFetchException | IOException ex) {
                rejected.add(new RejectedPhoto(url, "Unable to download: " + ex.getMessage()));
                continue;
            }
            EmbedResponse response = sidecarClient.embed(jpeg);
            String problem = checkModel(response);
            if (problem != null) {
                throw new FaceSidecarException(problem);
            }
            EmbeddedFace face = singleFace(response.faces());
            if (face == null) {
                rejected.add(new RejectedPhoto(url, response.faces() == null || response.faces().isEmpty()
                        ? "No face found" : "More than one face, use a photo with only this player"));
                continue;
            }
            accepted.add(new Accepted(url, face));
        }
        accepted = dropOutliers(accepted, rejected);
        if (!accepted.isEmpty()) {
            long removed = embeddingRepository.deleteByPlayerId(playerId);
            for (Accepted photo : accepted) {
                String sourceUrl = photo.url().length() > 2000 ? photo.url().substring(0, 2000) : photo.url();
                embeddingRepository.save(new PlayerFaceEmbedding(playerId, properties.getModel(), photo.face().embedding(),
                        sourceUrl, photo.face().detScore()));
            }
            log.atInfo().setMessage("Enrolled face references").addKeyValue("playerId", playerId)
                    .addKeyValue("enrolled", accepted.size()).addKeyValue("replaced", removed).log();
            return new EnrolmentResult(playerId, accepted.size(), properties.getMinReferences(), rejected);
        }
        return new EnrolmentResult(playerId, referenceCount(playerId), properties.getMinReferences(), rejected);
    }

    public int referenceCount(Integer playerId) {
        return embeddingRepository.findByPlayerIdAndModel(playerId, properties.getModel()).size();
    }

    /**
     * Delete all of the player's face embeddings.
     */
    @Transactional
    public long deleteReferences(Integer playerId) {
        long removed = embeddingRepository.deleteByPlayerId(playerId);
        log.atInfo().setMessage("Deleted face references").addKeyValue("playerId", playerId).addKeyValue("removed", removed).log();
        return removed;
    }

    private String checkModel(EmbedResponse response) {
        if (response == null) {
            return "Empty face sidecar response";
        }
        if (response.model() != null && !response.model().equals(properties.getModel())) {
            return "Face sidecar runs model " + response.model() + " but photo.face.model is " + properties.getModel();
        }
        return null;
    }

    /**
     * The only face, or the largest when it clearly dominates the photo. Null otherwise.
     */
    static EmbeddedFace singleFace(List<EmbeddedFace> faces) {
        if (faces == null || faces.isEmpty()) {
            return null;
        }
        List<EmbeddedFace> bySize = faces.stream()
                .sorted((a, b) -> Double.compare(FaceVectors.area(b.bbox()), FaceVectors.area(a.bbox()))).toList();
        if (bySize.size() == 1) {
            return bySize.get(0);
        }
        double largest = FaceVectors.area(bySize.get(0).bbox());
        double next = FaceVectors.area(bySize.get(1).bbox());
        return next == 0 || largest / next >= DOMINANT_FACE_AREA_RATIO ? bySize.get(0) : null;
    }

    /**
     * With three or more references, drop any that doesn't look like the others (likely the wrong person).
     */
    private List<Accepted> dropOutliers(List<Accepted> accepted, List<RejectedPhoto> rejected) {
        if (accepted.size() < 3) {
            return accepted;
        }
        List<Accepted> kept = new ArrayList<>();
        for (Accepted candidate : accepted) {
            double total = 0;
            for (Accepted other : accepted) {
                if (other != candidate) {
                    total += FaceVectors.cosine(candidate.face().embedding(), other.face().embedding());
                }
            }
            double meanSimilarity = total / (accepted.size() - 1);
            if (meanSimilarity < properties.getMinSimilarity()) {
                rejected.add(new RejectedPhoto(candidate.url(),
                        String.format("Face doesn't match the other reference photos (similarity %.2f)", meanSimilarity)));
            } else {
                kept.add(candidate);
            }
        }
        return kept;
    }
}
