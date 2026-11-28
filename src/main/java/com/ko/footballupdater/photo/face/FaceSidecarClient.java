package com.ko.footballupdater.photo.face;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ko.footballupdater.configuration.FaceMatchingProperties;
import com.ko.footballupdater.photo.face.FaceSidecarModels.EmbedResponse;
import com.ko.footballupdater.photo.face.FaceSidecarModels.MatchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * HTTP client for the stateless face sidecar. Every call has a connect and a read timeout and any failure
 * becomes a {@link FaceSidecarException}, so callers can skip the photo.
 */
@Component
public class FaceSidecarClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private FaceMatchingProperties properties;

    private volatile HttpClient httpClient;

    public FaceSidecarClient() {
    }

    public FaceSidecarClient(FaceMatchingProperties properties) {
        this.properties = properties;
    }

    /**
     * Faces in the image, largest first, each with its embedding.
     */
    public EmbedResponse embed(byte[] jpeg) throws FaceSidecarException {
        Multipart body = new Multipart().file("image", jpeg);
        return post("/embed", body, EmbedResponse.class);
    }

    /**
     * Compare every face in the image with the reference embeddings.
     */
    public MatchResponse match(byte[] jpeg, List<float[]> references) throws FaceSidecarException {
        String referencesJson;
        try {
            referencesJson = MAPPER.writeValueAsString(references);
        } catch (IOException ex) {
            throw new FaceSidecarException("Unable to encode references", ex);
        }
        Multipart body = new Multipart().file("image", jpeg).field("references", referencesJson);
        return post("/match", body, MatchResponse.class);
    }

    private <T> T post(String path, Multipart body, Class<T> responseType) throws FaceSidecarException {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(stripTrailingSlash(properties.getSidecarUrl()) + path))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMillis()))
                    .header("Content-Type", "multipart/form-data; boundary=" + body.boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.build()))
                    .build();
        } catch (IllegalArgumentException ex) {
            throw new FaceSidecarException("Invalid face sidecar URL", ex);
        }
        HttpResponse<byte[]> response;
        try {
            response = client().send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException ex) {
            throw new FaceSidecarException("Face sidecar unreachable or timed out: " + ex.getClass().getSimpleName(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FaceSidecarException("Interrupted calling face sidecar", ex);
        }
        if (response.statusCode() != 200) {
            throw new FaceSidecarException("Face sidecar returned HTTP " + response.statusCode() + ": "
                    + new String(response.body(), 0, Math.min(response.body().length, 300), StandardCharsets.UTF_8));
        }
        try {
            return MAPPER.readValue(response.body(), responseType);
        } catch (IOException ex) {
            throw new FaceSidecarException("Unreadable face sidecar response", ex);
        }
    }

    private HttpClient client() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .build();
                }
            }
        }
        return httpClient;
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static final class Multipart {
        private final String boundary = "----face" + UUID.randomUUID().toString().replace("-", "");
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Multipart file(String name, byte[] data) {
            write("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"image.jpg\"\r\n"
                    + "Content-Type: image/jpeg\r\n\r\n");
            out.writeBytes(data);
            write("\r\n");
            return this;
        }

        Multipart field(String name, String value) {
            write("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n");
            return this;
        }

        byte[] build() {
            write("--" + boundary + "--\r\n");
            return out.toByteArray();
        }

        private void write(String text) {
            out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
