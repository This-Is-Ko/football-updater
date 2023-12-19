package com.ko.footballupdater.runners;

import com.ko.footballupdater.configuration.ImageGeneratorProperies;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class FileAccessValidationRunner implements ApplicationRunner {

    @Autowired
    private ImageGeneratorProperies imageGeneratorProperies;

    @Override
    public void run(ApplicationArguments args) {
        validateFileAccess();
    }

    public void validateFileAccess() {
        File dir = new File(imageGeneratorProperies.getInputPath());
        File[] directoryListing = dir.listFiles();
        boolean isGenericBaseImageSet = false;
        if (directoryListing != null) {
            log.info(String.format("Found %d files in input directory: %s", directoryListing.length, imageGeneratorProperies.getInputPath()));
            for (File child : directoryListing) {
                try {
                    if (child.exists() && child.canRead()) {
                        log.debug("File access is valid for " + child.getAbsolutePath());
                        if (imageGeneratorProperies.getGenericBaseImageFile().equals(child.getName())) {
                            isGenericBaseImageSet = true;
                        }
                    }
                } catch (Exception e) {
                    log.error("File access is not valid for " + child.getAbsolutePath());
                }
            }
            if (!isGenericBaseImageSet) {
                log.warn("Generic base image file was not found");
            }
            log.info("Completed input directory file access check");
        } else {
            log.warn("Input directory contains no files");
        }
    }
}