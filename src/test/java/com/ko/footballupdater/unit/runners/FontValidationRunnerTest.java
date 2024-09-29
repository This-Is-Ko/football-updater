package com.ko.footballupdater.unit.runners;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ko.footballupdater.runners.FontValidationRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class FontValidationRunnerTest {

    @InjectMocks
    private FontValidationRunner fontValidationRunner;

    @Mock
    private ApplicationArguments applicationArguments;

    @BeforeAll
    public static void beforeAllSetup() {
        mockStatic(GraphicsEnvironment.class);
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void run_registerFontsAndValidateFontAvailability_shouldLogAvailableFonts() throws IOException, FontFormatException {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(FontValidationRunner.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        GraphicsEnvironment graphicsEnvironment = mock(GraphicsEnvironment.class);
        when(graphicsEnvironment.getAvailableFontFamilyNames()).thenReturn(new String[]{"Arial", "Times New Roman"});

        when(GraphicsEnvironment.getLocalGraphicsEnvironment()).thenReturn(graphicsEnvironment);

        fontValidationRunner.run(applicationArguments);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals("Font registered: Chakra Petch Bold", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals("Available fonts: [Arial, Times New Roman, Chakra Petch Bold]", logsList.get(1).getMessage());
    }

    @Test
    void validateFontAvailability_noFonts_shouldLogNoAvailableFonts() {
        // Set up log appender
        Logger fooLogger = (Logger) LoggerFactory.getLogger(FontValidationRunner.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        GraphicsEnvironment graphicsEnvironment = mock(GraphicsEnvironment.class);
        when(graphicsEnvironment.getAvailableFontFamilyNames()).thenReturn(new String[]{});

        // Mock static method call
        when(GraphicsEnvironment.getLocalGraphicsEnvironment()).thenReturn(graphicsEnvironment);

        // Act
        fontValidationRunner.validateFontAvailability();

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Available fonts: []", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
    }
}
