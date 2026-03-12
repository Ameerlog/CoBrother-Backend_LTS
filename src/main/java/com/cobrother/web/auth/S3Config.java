package com.cobrother.web.auth;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    public String accessKeyId;

    @Value("${cloud.aws.credentials.secret-key}")
    public String accessKeySecret;

    @Value("${cloud.aws.credentials.region}")
    public String region;

    @Value("${cloud.aws.venture.images-bucket}")
    private String imagesBucket;

    @Bean
    public AmazonS3 getS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, accessKeySecret);
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    @Bean
    public String ventureImagesBucket() {
        return imagesBucket;
    }

}
