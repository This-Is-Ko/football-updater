package com.ko.footballupdater.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ko.footballupdater.configuration.AmazonS3Properties;
import com.ko.footballupdater.configuration.ImageGeneratorProperies;
import com.ko.footballupdater.models.InstagramPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class AmazonS3Service {

    @Autowired
    private ImageGeneratorProperies imageGeneratorProperies;

    @Autowired
    private AmazonS3Properties amazonS3Properties;

    @Autowired
    private AmazonS3 s3Client;

    public void uploadtoS3(InstagramPost post) {
        if (!amazonS3Properties.isEnabled()) {
            return;
        }
        if (!post.getImagesFileNames().isEmpty()) {
            try {
                // Upload images and save urls
                // Overwrites any file with the same name
                for (String imageFileName : post.getImagesFileNames()) {
                    String filePath = imageGeneratorProperies.getOutputPath() + imageFileName;
                    File file = new File(filePath);
                    PutObjectRequest request = new PutObjectRequest(amazonS3Properties.getBucketName(),imageFileName, file)
                            .withCannedAcl(CannedAccessControlList.PublicRead);
                    s3Client.putObject(request);
                    String imageUrl = s3Client.getUrl(amazonS3Properties.getBucketName(), imageFileName).toString();
                    log.atInfo().setMessage(post.getPlayer().getName() + " - Successfully uploaded image " + imageFileName + " to S3 @ " + imageUrl).log();
                    post.getImagesS3Urls().add(imageUrl);

                    cleanUpFile(file);
                }
            } catch (AmazonServiceException ex) {
                // The call was transmitted successfully, but Amazon S3 couldn't process
                // it, so it returned an error response.
                log.warn(post.getPlayer().getName() + " - Error attempting to upload", ex);
            } catch (SdkClientException ex) {
                // Amazon S3 couldn't be contacted for a response, or the client
                // couldn't parse the response from Amazon S3.
                log.warn(post.getPlayer().getName() + " - Error attempting to upload", ex);
            }
        } else {
            log.atInfo().setMessage(post.getPlayer().getName() + " - No images to upload").log();
        }
    }

    // Delete files uploaded to S3
    private void cleanUpFile(File file) {
        if (file.delete()) {
            log.info("Deleted file: " + file.getAbsolutePath());
        } else {
            log.warn("Unable to delete file: " + file.getAbsolutePath());
        }
    }
}
