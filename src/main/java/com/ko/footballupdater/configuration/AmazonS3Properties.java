package com.ko.footballupdater.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class AmazonS3Properties {

    @NotNull
    private boolean enabled;

    @NotNull
    private String accessKey;

    @NotNull
    private String secretKey;

    @NotNull
    private String bucketName;

    private String objectKeyPrefix;

    @NotNull
    private String environment;

    @Bean
    public AmazonS3 s3Client() {
        // Set up S3 client
        log.info("Initialising S3 client");
        Regions clientRegion = Regions.AP_SOUTHEAST_2;
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(clientRegion)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}
