package com.ko.footballupdater.unit.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.ko.footballupdater.configuration.AmazonS3Properties;
import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.runners.FileAccessValidationRunner;
import com.ko.footballupdater.services.AmazonS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final Logger log = (Logger) LoggerFactory.getLogger(AmazonS3Service.class);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(amazonS3Properties.isEnabled()).thenReturn(true);
        when(amazonS3Properties.getEnvironment()).thenReturn("test-env");
        when(amazonS3Properties.getBucketName()).thenReturn("bucket-name");
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

    @Test
    public void testListFilesInS3BucketFolder() {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(AmazonS3Service.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        String folderPath = "some-folder/";
        S3ObjectSummary summary1 = new S3ObjectSummary();
        summary1.setKey("test-env/some-folder/file1.txt");
        S3ObjectSummary summary2 = new S3ObjectSummary();
        summary2.setKey("test-env/some-folder/file2.txt");
        List<S3ObjectSummary> objectSummaries = Arrays.asList(summary1, summary2);

        ListObjectsV2Result listObjectsV2Result = mock(ListObjectsV2Result.class);
        when(listObjectsV2Result.getObjectSummaries()).thenReturn(objectSummaries);
        when(listObjectsV2Result.isTruncated()).thenReturn(false);

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listObjectsV2Result);

        amazonS3Service.listFilesInS3BucketFolder(folderPath);

        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(" - " + summary1.getKey(), logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(" - " + summary2.getKey(), logsList.get(1).getMessage());
        assertEquals(Level.INFO, logsList.get(1).getLevel());
    }

    @Test
    public void testListFilesInS3BucketFolder_Truncated() {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(AmazonS3Service.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        String folderPath = "some-folder/";
        S3ObjectSummary summary1 = new S3ObjectSummary();
        summary1.setKey("test-env/some-folder/file1.txt");
        List<S3ObjectSummary> objectSummaries1 = List.of(summary1);

        S3ObjectSummary summary2 = new S3ObjectSummary();
        summary2.setKey("test-env/some-folder/file2.txt");
        List<S3ObjectSummary> objectSummaries2 = List.of(summary2);

        ListObjectsV2Result listObjectsV2Result1 = mock(ListObjectsV2Result.class);
        when(listObjectsV2Result1.getObjectSummaries()).thenReturn(objectSummaries1);
        when(listObjectsV2Result1.isTruncated()).thenReturn(true);
        when(listObjectsV2Result1.getNextContinuationToken()).thenReturn("token");

        ListObjectsV2Result listObjectsV2Result2 = mock(ListObjectsV2Result.class);
        when(listObjectsV2Result2.getObjectSummaries()).thenReturn(objectSummaries2);
        when(listObjectsV2Result2.isTruncated()).thenReturn(false);

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(listObjectsV2Result1)
                .thenReturn(listObjectsV2Result2);

        // Act
        amazonS3Service.listFilesInS3BucketFolder(folderPath);

        // Assert
        verify(s3Client, times(2)).listObjectsV2(any(ListObjectsV2Request.class));

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(" - " + summary1.getKey(), logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(" - " + summary2.getKey(), logsList.get(1).getMessage());
        assertEquals(Level.INFO, logsList.get(1).getLevel());
    }

    @Test
    public void testListFilesInS3BucketFolder_Exception() {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(AmazonS3Service.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        String folderPath = "some-folder/";
        RuntimeException exception = new RuntimeException("Test exception");

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(exception);

        amazonS3Service.listFilesInS3BucketFolder(folderPath);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Unable to list files in S3 bucket", logsList.get(0).getMessage());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
    }

}
