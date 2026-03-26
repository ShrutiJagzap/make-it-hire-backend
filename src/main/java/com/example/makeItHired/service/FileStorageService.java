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
    @Value("${app.upload.dir}")
    private String uploadDir;

    public Path storeFile(MultipartFile file, String subFolder) throws IOException {
        Path dir = Paths.get(uploadDir,subFolder);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(),target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
}
