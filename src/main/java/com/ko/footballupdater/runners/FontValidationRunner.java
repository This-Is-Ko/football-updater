package com.ko.footballupdater.runners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

@Slf4j
@Component
public class FontValidationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        validateFontAvailability();
    }

    public void validateFontAvailability() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();
        log.info("Available fonts: " + Arrays.toString(fonts));
    }
}