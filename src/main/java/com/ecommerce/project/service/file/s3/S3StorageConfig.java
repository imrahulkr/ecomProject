package com.ecommerce.project.service.file.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// Credentials come from the default AWS chain (AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY env
// vars, or an IAM role when running on EC2/ECS/Fargate) - matching the rest of the app's
// convention of secrets via env vars, never hardcoded in properties.
@Configuration
@ConditionalOnProperty(name = "app.file.storage.provider", havingValue = "s3")
public class S3StorageConfig {

    @Value("${app.file.storage.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
