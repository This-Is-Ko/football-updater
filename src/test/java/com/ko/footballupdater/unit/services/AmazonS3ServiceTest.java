package com.ko.footballupdater.unit.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ko.footballupdater.configuration.AmazonS3Properties;
import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.services.AmazonS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AmazonS3ServiceTest {

    @InjectMocks
    private AmazonS3Service amazonS3Service;

    @Mock
    private ImageGeneratorProperties imageGeneratorProperties;

    @Mock
    private AmazonS3Properties amazonS3Properties;

    @Mock
    private AmazonS3 s3Client;

    @Mock
    private Post post;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(amazonS3Properties.isEnabled()).thenReturn(true);
        when(imageGeneratorProperties.getOutputPath()).thenReturn("/path/to/output");

        Player player = new Player("Player1");
        when(post.getPlayer()).thenReturn(player);
    }

    @Test
    public void uploadToS3_imagesExist_successfullyUpload() throws Exception {
        when(post.getImagesFileNames()).thenReturn(List.of("image1.jpg", "image2.jpg"));
        when(post.getImagesUrls()).thenReturn(new ArrayList<>());

        File file1 = mock(File.class);
        File file2 = mock(File.class);
        String file1Path = "/path/to/output/image1.jpg";
        String file2Path = "/path/to/output/image2.jpg";
        when(file1.getAbsolutePath()).thenReturn(file1Path);
        when(file2.getAbsolutePath()).thenReturn(file2Path);
        when(file1.delete()).thenReturn(true);
        when(file2.delete()).thenReturn(true);

        URL s3Url1 = URI.create("https://s3.bucket/image1.jpg").toURL();
        URL s3Url2 = URI.create("https://s3.bucket/image2.jpg").toURL();

        when(s3Client.getUrl(amazonS3Properties.getBucketName(), "image1.jpg")).thenReturn(s3Url1);
        when(s3Client.getUrl(amazonS3Properties.getBucketName(), "image2.jpg")).thenReturn(s3Url2);

        amazonS3Service.uploadToS3(post);

        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class));

        verify(post, times(6)).getImagesUrls();
        assertEquals(2, post.getImagesUrls().size());
    }

    @Test
    public void uploadToS3_noImagesToUpload_noUpload() throws Exception {
        when(post.getImagesFileNames()).thenReturn(Collections.emptyList());
        when(post.getImagesUrls()).thenReturn(new ArrayList<>());

        amazonS3Service.uploadToS3(post);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class));
        assertEquals(0, post.getImagesUrls().size());
    }

    @Test
    public void uploadToS3_s3Disabled_noUpload() throws Exception {
        when(amazonS3Properties.isEnabled()).thenReturn(false);

        amazonS3Service.uploadToS3(post);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class));
    }

    @Test
    public void uploadToS3_amazonServiceException_exceptionThrown() {
        when(post.getImagesFileNames()).thenReturn(Collections.singletonList("image.jpg"));

        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/image.jpg");

        when(s3Client.getUrl(amazonS3Properties.getBucketName(), "image.jpg")).thenThrow(new AmazonServiceException("Simulated AmazonServiceException"));

        try {
            amazonS3Service.uploadToS3(post);
        } catch (Exception e) {
            assertEquals("Error attempting to upload", e.getMessage());
            assertEquals(0, post.getImagesUrls().size());
        }
    }

    @Test
    public void uploadToS3_sdkClientException_exceptionThrown() {
        when(post.getImagesFileNames()).thenReturn(Collections.singletonList("image.jpg"));

        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/image.jpg");

        when(s3Client.getUrl(amazonS3Properties.getBucketName(), "image.jpg")).thenThrow(new SdkClientException("Simulated AmazonServiceException"));

        try {
            amazonS3Service.uploadToS3(post);
        } catch (Exception e) {
            assertEquals("Error attempting to upload", e.getMessage());
            assertEquals(0, post.getImagesUrls().size());
        }
    }
}
