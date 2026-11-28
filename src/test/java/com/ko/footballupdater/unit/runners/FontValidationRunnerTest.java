package com.ko.footballupdater.unit.runners;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ko.footballupdater.runners.FontValidationRunner;
import com.ko.footballupdater.utils.FontRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Uses a stubbed FontRegistry rather than static mocking GraphicsEnvironment. A static mock of
 * GraphicsEnvironment is still instrumented while AWT initialises natively, which crashed the
 * JVM (SIGSEGV in jni_CallStaticVoidMethod) on Linux CI.
 */
public class FontValidationRunnerTest {

    @InjectMocks
    private FontValidationRunner fontValidationRunner;

    @Mock
    private ApplicationArguments applicationArguments;

    @Mock
    private FontRegistry fontRegistry;

    private AutoCloseable mocks;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger runnerLogger;

    @BeforeEach
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        runnerLogger = (Logger) LoggerFactory.getLogger(FontValidationRunner.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        runnerLogger.addAppender(listAppender);
    }

    @AfterEach
    public void teardown() throws Exception {
        runnerLogger.detachAppender(listAppender);
        mocks.close();
    }

    @Test
    void run_registerFontsAndValidateFontAvailability_shouldLogAvailableFonts() throws IOException, FontFormatException {
        when(fontRegistry.getAvailableFontFamilyNames()).thenReturn(new String[]{"Arial", "Times New Roman", "Chakra Petch Bold"});

        fontValidationRunner.run(applicationArguments);

        ArgumentCaptor<Font> fontCaptor = ArgumentCaptor.forClass(Font.class);
        verify(fontRegistry).registerFont(fontCaptor.capture());
        assertEquals("Chakra Petch Bold", fontCaptor.getValue().getFontName());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals("Font registered: Chakra Petch Bold", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals("Available fonts: [Arial, Times New Roman, Chakra Petch Bold]", logsList.get(1).getMessage());
    }

    @Test
    void validateFontAvailability_noFonts_shouldLogNoAvailableFonts() {
        when(fontRegistry.getAvailableFontFamilyNames()).thenReturn(new String[]{});

        fontValidationRunner.validateFontAvailability();

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Available fonts: []", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
    }
}
