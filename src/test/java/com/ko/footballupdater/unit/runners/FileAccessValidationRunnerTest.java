package com.ko.footballupdater.unit.runners;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.runners.FileAccessValidationRunner;
import com.ko.footballupdater.services.AmazonS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileAccessValidationRunnerTest {

    @InjectMocks
    private FileAccessValidationRunner fileAccessValidationRunner;

    @Mock
    private ImageGeneratorProperties imageGeneratorProperties;

    @Mock
    private AmazonS3Service amazonS3Service;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(imageGeneratorProperties.getInputPath()).thenReturn("/input");
        when(imageGeneratorProperties.getGenericBaseImageFile()).thenReturn("genericBaseImage.jpg");
    }

    @Test
    public void testValidateFileAccess() {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(FileAccessValidationRunner.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        fileAccessValidationRunner.validateFileAccess();

        verify(amazonS3Service, times(1)).listFilesInS3BucketFolder("/base-images/input/");
        verify(amazonS3Service, times(1)).listFilesInS3BucketFolder("/team-logos/");
        verify(amazonS3Service, times(1)).listFilesInS3BucketFolder("/icons/");

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("S3 Bucket path /base-images contains the following:", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals("S3 Bucket path /team-logos contains the following:", logsList.get(1).getMessage());
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals("S3 Bucket path /icons contains the following:", logsList.get(2).getMessage());
        assertEquals(Level.INFO, logsList.get(2).getLevel());
    }
}
