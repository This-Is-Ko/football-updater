package com.ko.footballupdater.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ko.footballupdater.configuration.AmazonS3Properties;
import com.ko.footballupdater.configuration.ImageGeneratorProperies;
import com.ko.footballupdater.models.Post;
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

    public void uploadToS3(Post post) throws Exception {
        uploadToS3(post, false);
    }

    public void uploadToS3(Post post, Boolean addToFront) throws Exception {
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
                    log.atInfo().setMessage("Successfully uploaded image " + imageFileName + " to S3 @ " + imageUrl).addKeyValue("player", post.getPlayer().getName()).log();

                    // Remove if url exists and add to end of list, keeps the order of the urls to be oldest to newest
                    // Prevents duplicates of the same url
                    post.getImagesUrls().remove(imageUrl);
                    // Add to front of image url list  if addToFront=true
                    // Allows the default order of images in post upload to be standoutImage then allStatImage
                    if (addToFront) {
                        post.getImagesUrls().add(0, imageUrl);
                    } else {
                        post.getImagesUrls().add(imageUrl);
                    }

                    cleanUpFile(file);
                }
            } catch (AmazonServiceException ex) {
                // The call was transmitted successfully, but Amazon S3 couldn't process
                // it, so it returned an error response.
                log.atWarn().setMessage("Error attempting to upload").setCause(ex).addKeyValue("player", post.getPlayer().getName()).log();
            } catch (SdkClientException ex) {
                // Amazon S3 couldn't be contacted for a response, or the client
                // couldn't parse the response from Amazon S3.
                log.atWarn().setMessage("Error attempting to upload").setCause(ex).addKeyValue("player", post.getPlayer().getName()).log();
                throw new Exception("Error attempting to upload");
            }
        } else {
            log.atInfo().setMessage(post.getPlayer().getName() + " - No images to upload").log();
        }
    }

    // Delete files uploaded to S3
    private void cleanUpFile(File file) {
        if (file.delete()) {
            log.atInfo().setMessage("Deleted file: " + file.getAbsolutePath()).log();
        } else {
            log.atWarn().setMessage("Unable to delete file: " + file.getAbsolutePath()).log();
        }
    }
}
