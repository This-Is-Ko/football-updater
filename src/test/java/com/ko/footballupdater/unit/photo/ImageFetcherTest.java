package com.ko.footballupdater.unit.photo;

import com.ko.footballupdater.configuration.PhotoProperties;
import com.ko.footballupdater.photo.ImageFetchException;
import com.ko.footballupdater.photo.ImageFetcher;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageFetcherTest {

    private HttpServer server;
    private PhotoProperties photoProperties;
    private ImageFetcher imageFetcher;
    private String baseUrl;

    private static byte[] jpeg(int width, int height) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }

    private void serve(String path, int status, String contentType, byte[] body, String location) {
        server.createContext(path, exchange -> {
            if (contentType != null) {
                exchange.getResponseHeaders().add("Content-Type", contentType);
            }
            if (location != null) {
                exchange.getResponseHeaders().add("Location", location);
            }
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    @BeforeEach
    public void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        photoProperties = new PhotoProperties();
        // The test server is on loopback
        photoProperties.setAllowPrivateAddresses(true);
        photoProperties.setDownloadTimeoutSeconds(5);
        imageFetcher = new ImageFetcher(photoProperties);
    }

    @AfterEach
    public void teardown() {
        server.stop(0);
    }

    @Test
    public void fetchImage_validJpeg_decoded() throws Exception {
        serve("/photo.jpg", 200, "image/jpeg", jpeg(300, 200), null);

        BufferedImage image = imageFetcher.fetchImage(baseUrl + "/photo.jpg");

        assertEquals(300, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    public void fetchImage_redirect_followedAndRevalidated() throws Exception {
        serve("/photo.jpg", 200, "image/jpeg", jpeg(50, 50), null);
        serve("/old", 302, null, new byte[0], "/photo.jpg");

        assertEquals(50, imageFetcher.fetchImage(baseUrl + "/old").getWidth());
    }

    @Test
    public void fetchImage_redirectToFileScheme_rejected() {
        serve("/old", 302, null, new byte[0], "file:///etc/passwd");

        assertThrows(ImageFetchException.class, () -> imageFetcher.fetchImage(baseUrl + "/old"));
    }

    @Test
    public void fetchImage_notAnImage_rejected() {
        serve("/page", 200, "text/html", "<html></html>".getBytes(), null);

        ImageFetchException ex = assertThrows(ImageFetchException.class, () -> imageFetcher.fetchImage(baseUrl + "/page"));
        assertTrue(ex.getMessage().contains("not an image"));
    }

    @Test
    public void fetchImage_overSizeLimit_rejected() throws Exception {
        serve("/big.jpg", 200, "image/jpeg", jpeg(400, 400), null);
        photoProperties.setMaxDownloadBytes(500);

        ImageFetchException ex = assertThrows(ImageFetchException.class, () -> imageFetcher.fetchImage(baseUrl + "/big.jpg"));
        assertTrue(ex.getMessage().contains("larger than"));
    }

    @Test
    public void fetchImage_overPixelLimit_rejectedBeforeDecoding() throws Exception {
        serve("/wide.jpg", 200, "image/jpeg", jpeg(400, 400), null);
        photoProperties.setMaxImagePixels(10_000);

        ImageFetchException ex = assertThrows(ImageFetchException.class, () -> imageFetcher.fetchImage(baseUrl + "/wide.jpg"));
        assertTrue(ex.getMessage().contains("dimensions"));
    }

    @Test
    public void fetchImage_notFound_rejected() {
        serve("/missing", 404, "text/plain", "nope".getBytes(), null);

        assertThrows(ImageFetchException.class, () -> imageFetcher.fetchImage(baseUrl + "/missing"));
    }

    @Test
    public void fetchImage_corruptImage_rejected() {
        serve("/broken.jpg", 200, "image/jpeg", "not really a jpeg".getBytes(), null);

        assertThrows(ImageFetchException.class, () -> imageFetcher.fetchImage(baseUrl + "/broken.jpg"));
    }

    @Test
    public void fetchImage_badSchemesAndUrls_rejected() {
        for (String url : new String[]{"file:///etc/passwd", "ftp://example.com/a.jpg", "javascript:alert(1)", "", null,
                "https://user:pass@example.com/a.jpg", "not a url"}) {
            assertThrows(ImageFetchException.class, () -> imageFetcher.fetchImage(url), String.valueOf(url));
        }
    }

    @Test
    public void fetchImage_privateAddressesBlockedByDefault() {
        photoProperties.setAllowPrivateAddresses(false);
        for (String url : new String[]{baseUrl + "/photo.jpg", "http://localhost/a.jpg", "http://169.254.169.254/latest/meta-data",
                "http://10.0.0.5/a.jpg", "http://192.168.1.1/a.jpg", "http://[::1]/a.jpg"}) {
            ImageFetchException ex = assertThrows(ImageFetchException.class, () -> imageFetcher.fetchImage(url), url);
            assertTrue(ex.getMessage().contains("private or local"), url + ": " + ex.getMessage());
        }
    }
}
