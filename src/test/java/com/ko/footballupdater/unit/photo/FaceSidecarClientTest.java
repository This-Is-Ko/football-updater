package com.ko.footballupdater.unit.photo;

import com.ko.footballupdater.configuration.FaceMatchingProperties;
import com.ko.footballupdater.photo.face.FaceSidecarClient;
import com.ko.footballupdater.photo.face.FaceSidecarException;
import com.ko.footballupdater.photo.face.FaceSidecarModels.MatchResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FaceSidecarClientTest {

    private HttpServer server;
    private FaceMatchingProperties properties;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseJson = "{}";
    private volatile long delayMillis = 0;

    @BeforeEach
    public void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        properties = new FaceMatchingProperties();
        properties.setSidecarUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        properties.setReadTimeoutMillis(1000);
    }

    @AfterEach
    public void teardown() {
        server.stop(0);
    }

    @Test
    public void match_parsesResponseAndSendsReferences() throws Exception {
        responseJson = """
                {"model":"buffalo_l","faceCount":1,"subject":{"index":0,"bbox":[1,2,30,40],"detScore":0.9,"maxSimilarity":0.61,"meanSimilarity":0.5},
                 "bestMatch":{"index":0,"bbox":[1,2,30,40],"detScore":0.9,"maxSimilarity":0.61,"meanSimilarity":0.5},"faces":[],"extra":true}
                """;

        MatchResponse response = new FaceSidecarClient(properties).match(new byte[]{1, 2, 3}, List.of(new float[]{0.5f, -0.25f}));

        assertEquals(0.61, response.subject().maxSimilarity(), 1e-9);
        assertEquals(1, response.faceCount());
        assertTrue(requestBody.get().contains("name=\"references\""));
        assertTrue(requestBody.get().contains("[[0.5,-0.25]]"));
        assertTrue(requestBody.get().contains("name=\"image\""));
    }

    @Test
    public void embed_parsesEmbeddings() throws Exception {
        responseJson = "{\"model\":\"buffalo_l\",\"faces\":[{\"bbox\":[0,0,10,10],\"detScore\":0.8,\"embedding\":[0.1,0.2]}]}";

        var response = new FaceSidecarClient(properties).embed(new byte[]{1});

        assertEquals(2, response.faces().get(0).embedding().length);
    }

    @Test
    public void httpError_throws() {
        status = 500;
        responseJson = "{\"detail\":\"boom\"}";

        FaceSidecarException ex = assertThrows(FaceSidecarException.class, () -> new FaceSidecarClient(properties).embed(new byte[]{1}));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    public void slowSidecar_timesOut() {
        delayMillis = 3000;
        properties.setReadTimeoutMillis(200);

        long start = System.currentTimeMillis();
        assertThrows(FaceSidecarException.class, () -> new FaceSidecarClient(properties).embed(new byte[]{1}));
        assertTrue(System.currentTimeMillis() - start < 2500);
    }

    @Test
    public void sidecarDown_throws() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }
        properties.setSidecarUrl("http://127.0.0.1:" + freePort);

        assertThrows(FaceSidecarException.class, () -> new FaceSidecarClient(properties).embed(new byte[]{1}));
    }

    @Test
    public void garbageResponse_throws() {
        responseJson = "not json";

        assertThrows(FaceSidecarException.class, () -> new FaceSidecarClient(properties).embed(new byte[]{1}));
    }
}
