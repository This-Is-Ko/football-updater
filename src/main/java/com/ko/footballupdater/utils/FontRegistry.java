package com.ko.footballupdater.utils;

import java.awt.Font;

/**
 * Wraps the AWT font registry so callers don't depend on GraphicsEnvironment directly.
 * Tests can stub this instead of static mocking GraphicsEnvironment, which interferes with
 * native AWT initialisation and crashed the JVM on Linux.
 */
public interface FontRegistry {

    /**
     * @return true if the font was registered
     */
    boolean registerFont(Font font);

    String[] getAvailableFontFamilyNames();
}
