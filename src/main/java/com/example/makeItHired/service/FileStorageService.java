package com.example.makeItHired.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public Path storeFile(MultipartFile file, String subFolder) throws IOException {
        Path dir = Paths.get(uploadDir).toAbsolutePath().resolve(subFolder);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            System.out.println("Created directory:" + dir.toString());
        }
        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        Path targetPath = dir.resolve(filename);
        Files.copy(file.getInputStream(),targetPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File stored at:" + targetPath.toString());
        return targetPath;
    }
}
