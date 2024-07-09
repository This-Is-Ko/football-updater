package com.ko.footballupdater.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.ko.footballupdater.configuration.AmazonS3Properties;
import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.utils.LogHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;

@Slf4j
@Service
public class AmazonS3Service {

    @Autowired
    private ImageGeneratorProperties imageGeneratorProperties;

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
                    String filePath = imageGeneratorProperties.getOutputPath() + "/" + imageFileName;
                    File file = new File(filePath);
                    // Save to specific path/key prefix if configured
                    String imageKey = amazonS3Properties.getObjectKeyPrefix() != null ? amazonS3Properties.getEnvironment() + "/" + amazonS3Properties.getObjectKeyPrefix() + imageFileName : imageFileName;
                    PutObjectRequest request = new PutObjectRequest(amazonS3Properties.getBucketName(), imageKey, file)
                            .withCannedAcl(CannedAccessControlList.PublicRead);
                    s3Client.putObject(request);
                    String imageUrl = s3Client.getUrl(amazonS3Properties.getBucketName(), imageKey).toString();
                    LogHelper.logWithSubject(log.atInfo().setMessage("Successfully uploaded image " + imageFileName + " to S3 @ " + imageUrl), post);

                    // Remove if url exists and add to end of list, keeps the order of the urls to be oldest to newest
                    // Prevents duplicates of the same url
                    if (post.getImagesUrls() != null) {
                        post.getImagesUrls().remove(imageUrl);
                    } else {
                        post.setImagesUrls(new ArrayList<>());
                    }
                    // Add to front of image url list  if addToFront=true
                    // Allows the default order of images in post upload to be standoutImage then allStatImage)
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
                LogHelper.logWithSubject(log.atWarn().setMessage("Error attempting to upload").setCause(ex), post);
                throw new Exception("Error attempting to upload");
            } catch (SdkClientException ex) {
                // Amazon S3 couldn't be contacted for a response, or the client
                // couldn't parse the response from Amazon S3.
                LogHelper.logWithSubject(log.atWarn().setMessage("Error attempting to upload").setCause(ex), post);
                throw new Exception("Error attempting to upload");
            }
        } else {
            log.atInfo().setMessage(post.getPlayer().getName() + " - No images to upload").log();
        }
    }

    public void listFilesInS3BucketFolder(String folderPath) {
        try {
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
            listObjectsV2Request.setBucketName(amazonS3Properties.getBucketName());
            listObjectsV2Request.withPrefix(amazonS3Properties.getEnvironment() + folderPath);

            ListObjectsV2Result listObjectsV2Result;
            do {
                listObjectsV2Result = s3Client.listObjectsV2(listObjectsV2Request);
                for (S3ObjectSummary s3ObjectSummary : listObjectsV2Result.getObjectSummaries()) {
                    log.atInfo().setMessage(" - " + s3ObjectSummary.getKey()).log();
                }
                // If there are more than maxKeys keys in the bucket, get a continuation token
                String token = listObjectsV2Result.getNextContinuationToken();
                listObjectsV2Request.setContinuationToken(token);
            } while (listObjectsV2Result.isTruncated());
        } catch (Exception e) {
            log.atInfo().setCause(e).log();
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
