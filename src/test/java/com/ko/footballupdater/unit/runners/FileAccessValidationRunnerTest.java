package com.ko.footballupdater.unit.runners;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ko.footballupdater.configuration.ImageGeneratorProperies;
import com.ko.footballupdater.runners.FileAccessValidationRunner;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileAccessValidationRunnerTest {

    @InjectMocks
    private FileAccessValidationRunner fileAccessValidationRunner;

    @Mock
    private ImageGeneratorProperies imageGeneratorProperies;

    @Mock
    private ApplicationArguments applicationArguments;

    @Mock
    private File directory;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(imageGeneratorProperies.getInputPath()).thenReturn("src/test/resources/input");
        when(imageGeneratorProperies.getGenericBaseImageFile()).thenReturn("genericBaseImage.jpg");
    }

    @Test
    void run_validateFileAccess_shouldLogValidFileAccess() {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(FileAccessValidationRunner.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        File validFile1 = mock(File.class);
        File validFile2 = mock(File.class);
        when(directory.listFiles()).thenReturn(new File[]{validFile1, validFile2});
        when(validFile1.getName()).thenReturn("file");
        when(validFile2.getName()).thenReturn("genericBaseImage.jpg");
        when(validFile1.exists()).thenReturn(true);
        when(validFile2.exists()).thenReturn(true);
        when(validFile1.canRead()).thenReturn(true);
        when(validFile2.canRead()).thenReturn(true);

        fileAccessValidationRunner.validateFileAccess(directory);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Found 2 files in input directory: src/test/resources/input", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals("Completed input directory file access check", logsList.get(1).getMessage());
        assertEquals(Level.INFO, logsList.get(1).getLevel());
    }

    @Test
    void run_validateFileAccess_missingGenericBaseImageFile() {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(FileAccessValidationRunner.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        File validFile1 = mock(File.class);
        when(directory.listFiles()).thenReturn(new File[]{validFile1});
        when(validFile1.getName()).thenReturn("file");
        when(validFile1.exists()).thenReturn(true);
        when(validFile1.canRead()).thenReturn(true);


        fileAccessValidationRunner.validateFileAccess(directory);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Found 1 files in input directory: src/test/resources/input", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals("Generic base image file was not found", logsList.get(1).getMessage());
        assertEquals(Level.WARN, logsList.get(1).getLevel());
        assertEquals("Completed input directory file access check", logsList.get(2).getMessage());
        assertEquals(Level.INFO, logsList.get(2).getLevel());
    }

    @Test
    void run_validateFileAccess_filesAreNotAccessible() {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(FileAccessValidationRunner.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        File validFile1 = mock(File.class);
        File validFile2 = mock(File.class);
        when(directory.listFiles()).thenReturn(new File[]{validFile1, validFile2});
        when(validFile1.getName()).thenReturn("file");
        when(validFile2.getName()).thenReturn("genericBaseImage.jpg");
        when(validFile1.getAbsolutePath()).thenReturn("absolute/path/file1");
        when(validFile2.getAbsolutePath()).thenReturn("absolute/path/file2");
        when(validFile1.exists()).thenReturn(true);
        when(validFile2.exists()).thenReturn(true);
        when(validFile1.canRead()).thenReturn(false);
        when(validFile2.canRead()).thenReturn(false);

        fileAccessValidationRunner.validateFileAccess(directory);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Found 2 files in input directory: src/test/resources/input", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals("File access is not valid for absolute/path/file1", logsList.get(1).getMessage());
        assertEquals(Level.ERROR, logsList.get(1).getLevel());
        assertEquals("File access is not valid for absolute/path/file2", logsList.get(2).getMessage());
        assertEquals(Level.ERROR, logsList.get(2).getLevel());
        assertEquals("Generic base image file was not found", logsList.get(3).getMessage());
        assertEquals(Level.WARN, logsList.get(3).getLevel());
        assertEquals("Completed input directory file access check", logsList.get(4).getMessage());
        assertEquals(Level.INFO, logsList.get(4).getLevel());
    }

}
