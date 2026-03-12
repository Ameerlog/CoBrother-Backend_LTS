package com.cobrother.web.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    @Autowired
    private AmazonS3 s3Client;

    @Value("${cloud.aws.venture.images-bucket}")
    private String bucket;

    @Value("${cloud.aws.credentials.region}")
    private String region;

    public String uploadVentureImage(MultipartFile file, Long ventureId) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String key = "venture-images/" + UUID.randomUUID() + extension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        s3Client.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), metadata));
        String st="https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        System.out.println(st);
        return st;
    }

    public void deleteVentureImage(String imageUrl) {
        try {
            // Extract key from URL
            String key = imageUrl.substring(imageUrl.indexOf("venture-images/"));
            s3Client.deleteObject(bucket, key);
        } catch (Exception ignored) {}
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}