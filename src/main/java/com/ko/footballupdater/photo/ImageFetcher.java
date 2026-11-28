package com.ko.footballupdater.photo;

import com.ko.footballupdater.configuration.PhotoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

/**
 * Downloads images from URLs given by people or photo sources, with limits:
 * http/https only, no private/loopback/link-local hosts (including after redirects), a timeout,
 * a maximum download size and a maximum decoded image size.
 */
@Slf4j
@Component
public class ImageFetcher {

    private static final int MAX_REDIRECTS = 3;

    @Autowired
    private PhotoProperties photoProperties;

    public ImageFetcher() {
    }

    // For tests
    public ImageFetcher(PhotoProperties photoProperties) {
        this.photoProperties = photoProperties;
    }

    public BufferedImage fetchImage(String url) throws ImageFetchException {
        return decode(fetchBytes(url));
    }

    public byte[] fetchBytes(String url) throws ImageFetchException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(photoProperties.getDownloadTimeoutSeconds()))
                .build();
        URI uri = validateUri(url);
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(photoProperties.getDownloadTimeoutSeconds()))
                    .header("Accept", "image/*")
                    .GET()
                    .build();
            HttpResponse<InputStream> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (IOException ex) {
                throw new ImageFetchException("Unable to download image: " + ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ImageFetchException("Image download interrupted", ex);
            }

            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                closeQuietly(response.body());
                Optional<String> location = response.headers().firstValue("Location");
                if (location.isEmpty()) {
                    throw new ImageFetchException("Redirect without a location");
                }
                // Each redirect target is validated again
                uri = validateUri(uri.resolve(location.get()).toString());
                continue;
            }
            try (InputStream body = response.body()) {
                if (status != 200) {
                    throw new ImageFetchException("Image download failed with status " + status);
                }
                String contentType = response.headers().firstValue("Content-Type").orElse("");
                if (!contentType.isEmpty() && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                    throw new ImageFetchException("URL is not an image (Content-Type " + contentType + ")");
                }
                long declaredLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (declaredLength > photoProperties.getMaxDownloadBytes()) {
                    throw new ImageFetchException("Image is larger than " + photoProperties.getMaxDownloadBytes() + " bytes");
                }
                return readLimited(body, photoProperties.getMaxDownloadBytes());
            } catch (IOException ex) {
                throw new ImageFetchException("Unable to read image: " + ex.getMessage(), ex);
            }
        }
        throw new ImageFetchException("Too many redirects");
    }

    URI validateUri(String url) throws ImageFetchException {
        if (url == null || url.isBlank()) {
            throw new ImageFetchException("Image URL is empty");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            throw new ImageFetchException("Invalid image URL");
        }
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new ImageFetchException("Image URL must use http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ImageFetchException("Image URL has no host");
        }
        if (uri.getUserInfo() != null) {
            throw new ImageFetchException("Image URL must not contain credentials");
        }
        if (!photoProperties.isAllowPrivateAddresses()) {
            try {
                for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                    if (isPrivate(address)) {
                        throw new ImageFetchException("Image URL points to a private or local address");
                    }
                }
            } catch (UnknownHostException ex) {
                throw new ImageFetchException("Unknown image host " + uri.getHost());
            }
        }
        return uri;
    }

    static boolean isPrivate(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            // Unique local addresses fc00::/7
            return (bytes[0] & 0xfe) == 0xfc;
        }
        // Carrier-grade NAT 100.64.0.0/10
        return (bytes[0] & 0xff) == 100 && (bytes[1] & 0xc0) == 64;
    }

    private static byte[] readLimited(InputStream body, long maxBytes) throws IOException, ImageFetchException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = body.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new ImageFetchException("Image is larger than " + maxBytes + " bytes");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Decode image bytes, checking the dimensions before decoding the pixels.
     */
    public BufferedImage decode(byte[] bytes) throws ImageFetchException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new ImageFetchException("Unreadable image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ImageFetchException("Unsupported image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > photoProperties.getMaxImagePixels()) {
                    throw new ImageFetchException("Image dimensions are too large");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new ImageFetchException("Unreadable image");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new ImageFetchException("Unreadable image: " + ex.getMessage(), ex);
        }
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // nothing to do
        }
    }
}
