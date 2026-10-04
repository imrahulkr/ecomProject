package com.ecommerce.project.service.file.s3;

import com.ecommerce.project.service.FileService;
import com.ecommerce.project.service.file.ImageUploadValidator;
import com.ecommerce.project.service.file.ValidatedImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

// Production storage path - see LocalDiskFileService for why local disk doesn't work once
// there's more than one instance. `path` (the same "image/" prefix ProductServiceImpl already
// passes for local storage) doubles as the S3 key prefix, so image.base.url just needs to point
// at the bucket's public URL (or a CloudFront distribution in front of it) for the two to line
// up - see application-prod.properties.example.
@Service
@ConditionalOnProperty(name = "app.file.storage.provider", havingValue = "s3")
public class S3FileService implements FileService {

    private final S3Client s3Client;
    private final ImageUploadValidator validator;

    @Value("${app.file.storage.s3.bucket}")
    private String bucket;

    public S3FileService(S3Client s3Client, ImageUploadValidator validator) {
        this.s3Client = s3Client;
        this.validator = validator;
    }

    @Override
    public String uploadImage(String path, MultipartFile image) {
        ValidatedImage validated = validator.validate(image);
        String fileName = UUID.randomUUID() + "." + validated.extension();
        String key = path.endsWith("/") ? path + fileName : path + "/" + fileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(validated.contentType())
                .contentLength((long) validated.content().length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(validated.content()));
        return fileName;
    }
}
