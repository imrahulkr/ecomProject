package com.ecommerce.project.service.file;

import com.ecommerce.project.exceptions.APIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

// Shared by every FileService implementation (local disk, S3, ...) so upload rules stay
// identical regardless of where the bytes end up - see LocalDiskFileService / S3FileService.
@Component
public class ImageUploadValidator {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    @Value("${app.upload.image.max-size-bytes:5242880}")
    private long maxFileSizeBytes;

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new APIException("Image file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new APIException("Image exceeds the maximum allowed size of " + (maxFileSizeBytes / (1024 * 1024)) + "MB");
        }

        String contentType = file.getContentType();
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new APIException("Unsupported image type - allowed types are JPEG, PNG and WEBP");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new APIException("Could not read uploaded file", e);
        }

        if (!isGenuineImage(contentType, content)) {
            throw new APIException("Uploaded file is not a valid " + extension.toUpperCase() + " image");
        }

        return new ValidatedImage(content, contentType, extension);
    }

    // Inspects the actual bytes rather than trusting the client-supplied Content-Type header -
    // otherwise an HTML/script polyglot renamed to product.jpg with a spoofed Content-Type would
    // pass straight through to storage that's served back out publicly. JPEG/PNG get a full
    // ImageIO decode; WEBP isn't supported by the JDK's built-in ImageIO plugins, so it gets a
    // RIFF/WEBP container-signature check instead - weaker than a full decode, but still rejects
    // anything that isn't actually a WebP container.
    private boolean isGenuineImage(String contentType, byte[] content) {
        if ("image/webp".equals(contentType)) {
            return content.length >= 12
                    && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                    && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(content)) != null;
        } catch (IOException e) {
            return false;
        }
    }
}
