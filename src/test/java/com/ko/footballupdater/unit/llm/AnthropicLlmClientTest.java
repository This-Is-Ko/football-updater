package com.ko.footballupdater.unit.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.AnthropicLlmClient;
import com.ko.footballupdater.llm.LlmException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real SDK against a local stub server, checking request serialization and
 * structured response parsing work with the project's Jackson/Kotlin versions.
 */
public class AnthropicLlmClientTest {

    public record Sample(String name, List<Integer> values) {
    }

    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private volatile String responseBody;
    private LlmProperties llmProperties;
    private AnthropicLlmClient llmClient;

    @BeforeEach
    public void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        llmProperties = new LlmProperties();
        llmProperties.setEnabled(true);
        llmProperties.setModel("claude-sonnet-5-5");
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey("test-key")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .maxRetries(0)
                .build();
        llmClient = new AnthropicLlmClient(llmProperties, client);
    }

    @AfterEach
    public void teardown() {
        server.stop(0);
    }

    private static String messageResponse(String text, String stopReason) {
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-sonnet-5-5\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"" + escaped + "\"}],"
                + "\"stop_reason\":\"" + stopReason + "\",\"stop_sequence\":null,"
                + "\"usage\":{\"input_tokens\":10,\"output_tokens\":5}}";
    }

    @Test
    public void generateStructured_validResponse_parsesIntoRecord() throws Exception {
        responseBody = messageResponse("{\"name\":\"abc\",\"values\":[1,2]}", "end_turn");

        Sample sample = llmClient.generateStructured("system prompt", "user prompt", Sample.class);

        assertEquals("abc", sample.name());
        assertEquals(List.of(1, 2), sample.values());
        String body = requestBody.get();
        assertTrue(body.contains("\"model\":\"claude-sonnet-5-5\""));
        assertTrue(body.contains("\"output_config\""));
        assertTrue(body.contains("\"json_schema\""));
        assertTrue(body.contains("system prompt"));
    }

    @Test
    public void generateStructured_refusal_throws() {
        responseBody = messageResponse("", "refusal");

        assertThrows(LlmException.class, () -> llmClient.generateStructured("s", "u", Sample.class));
    }

    @Test
    public void generateStructured_disabled_throws() {
        llmProperties.setEnabled(false);

        assertThrows(LlmException.class, () -> llmClient.generateStructured("s", "u", Sample.class));
    }

    @Test
    public void generateStructured_serverError_throwsLlmException() {
        server.removeContext("/v1/messages");
        server.createContext("/v1/messages", exchange -> {
            byte[] bytes = "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"boom\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThrows(LlmException.class, () -> llmClient.generateStructured("s", "u", Sample.class));
    }
}
