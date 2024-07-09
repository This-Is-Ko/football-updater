package com.ko.footballupdater.runners;

import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.services.AmazonS3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static com.ko.footballupdater.utils.ImageGeneratorConstants.BASE_IMAGE_DIRECTORY;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.ICON_DIRECTORY;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.TEAM_LOGO_DIRECTORY;

@Slf4j
@Component
public class FileAccessValidationRunner implements ApplicationRunner {

    @Autowired
    private ImageGeneratorProperties imageGeneratorProperties;

    @Autowired
    private AmazonS3Service amazonS3Service;

    @Override
    public void run(ApplicationArguments args) {
        validateFileAccess();
    }

    public void validateFileAccess() {
        log.atInfo().setMessage("S3 Bucket path " + BASE_IMAGE_DIRECTORY + " contains the following: ").log();
        amazonS3Service.listFilesInS3BucketFolder(BASE_IMAGE_DIRECTORY + imageGeneratorProperties.getInputPath() + "/");
        log.atInfo().setMessage("S3 Bucket path " + TEAM_LOGO_DIRECTORY + " contains the following: ").log();
        amazonS3Service.listFilesInS3BucketFolder(TEAM_LOGO_DIRECTORY + "/");
        log.atInfo().setMessage("S3 Bucket path " + ICON_DIRECTORY + " contains the following: ").log();
        amazonS3Service.listFilesInS3BucketFolder(ICON_DIRECTORY + "/");
    }
}