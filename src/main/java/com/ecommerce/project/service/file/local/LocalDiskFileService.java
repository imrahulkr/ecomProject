package com.ecommerce.project.service.file.local;

import com.ecommerce.project.service.FileService;
import com.ecommerce.project.service.file.ImageUploadValidator;
import com.ecommerce.project.service.file.ValidatedImage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// Default storage - writes validated images to local disk. Fine for a single local/dev
// instance, but the disk isn't shared across instances/containers and doesn't survive a
// redeploy - see S3FileService (app.file.storage.provider=s3) for the production path.
@Service
@ConditionalOnProperty(name = "app.file.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalDiskFileService implements FileService {

    private final ImageUploadValidator validator;

    public LocalDiskFileService(ImageUploadValidator validator) {
        this.validator = validator;
    }

    @Override
    public String uploadImage(String path, MultipartFile image) throws IOException {
        ValidatedImage validated = validator.validate(image);
        String fileName = UUID.randomUUID() + "." + validated.extension();

        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        Files.write(Path.of(path, fileName), validated.content());
        return fileName;
    }
}
