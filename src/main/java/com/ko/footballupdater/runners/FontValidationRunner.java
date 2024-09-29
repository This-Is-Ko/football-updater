package com.ko.footballupdater.runners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Slf4j
@Component
public class FontValidationRunner implements ApplicationRunner {

    final String FONTS_PATH = "classpath:fonts/*.{ttf,otf}";

    @Override
    public void run(ApplicationArguments args) throws IOException, FontFormatException {
        registerFonts();
        validateFontAvailability();
    }

    public void registerFonts() throws IOException, FontFormatException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(FONTS_PATH);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        // Register each font
        for (Resource resource : resources) {
            InputStream fontStream = resource.getInputStream();
            // Create the font from the input stream
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);

            // Register the font with the graphics environment
            ge.registerFont(font);

            log.info("Font registered: " + font.getFontName());
        }
    }

    public void validateFontAvailability() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();
        log.info("Available fonts: " + Arrays.toString(fonts));
    }
}