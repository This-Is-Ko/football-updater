package com.ko.footballupdater.utils;

import org.springframework.stereotype.Component;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * FontRegistry backed by the local GraphicsEnvironment.
 */
@Component
public class LocalFontRegistry implements FontRegistry {

    @Override
    public boolean registerFont(Font font) {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
    }

    @Override
    public String[] getAvailableFontFamilyNames() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }
}
