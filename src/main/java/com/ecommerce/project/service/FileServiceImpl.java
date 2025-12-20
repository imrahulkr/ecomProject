package com.ecommerce.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public String uploadImage(String path, MultipartFile image) throws IOException {
        // File names of current/original file
        String originalFilename = image.getOriginalFilename();
        // Generate a unique fine name (UUID)
        String randomID = UUID.randomUUID().toString();
        String fileName = randomID + originalFilename.substring(originalFilename.lastIndexOf("."));
        String filePath = path + File.separator +fileName;
        // Check if path exist or Create
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdir();
        }
        // Upload to server
        Files.copy(image.getInputStream(), Paths.get(filePath));
        // returing file name
        return fileName;
    }
}
