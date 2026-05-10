package com.example.makeItHired.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {
    @Value("${firebase.storage.enabled:true}")
    private boolean firebaseEnabled;

    @Value("${firebase.bucket.name:}")
    private String bucketName;

    @Value("${firebase.service.account.path:firebase-service-account.json}")
    private String serviceAccountPath;

    private Bucket bucket;

    @PostConstruct
    public void init() {
        if (!firebaseEnabled) {
            System.out.println("⚠️ Firebase storage disabled. Using local storage.");
            return;
        }

        try {
            // Initialize Firebase Admin SDK
            InputStream serviceAccount = getClass().getClassLoader()
                    .getResourceAsStream(serviceAccountPath);

            if (serviceAccount == null) {
                // Try from file system
                serviceAccount = new FileInputStream(serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket(bucketName)
                    .build();

            FirebaseApp.initializeApp(options);
            bucket = StorageClient.getInstance().bucket();
            System.out.println("✅ Firebase Storage initialized successfully!");
            System.out.println("📁 Bucket: " + bucketName);

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Firebase Storage: " + e.getMessage());
            firebaseEnabled = false;
        }
    }

    /**
     * Upload file to Firebase Storage
     * @param file Multipart file to upload
     * @param folder Folder name (e.g., "resumes", "id_photos")
     * @param userId User ID for organization
     * @return Download URL of uploaded file
     */
    public String uploadFile(MultipartFile file, String folder, Long userId) throws IOException {
        if (!firebaseEnabled || bucket == null) {
            System.out.println("Using local storage fallback");
            return null;
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";

            String fileName = folder + "/" + userId + "_" + System.currentTimeMillis() + "_"
                    + UUID.randomUUID().toString().substring(0, 8) + extension;

            // Upload to Firebase Storage
            Blob blob = bucket.create(
                    fileName,
                    file.getInputStream(),
                    file.getContentType()
            );

            // Generate signed URL (valid for 7 days)
            String signedUrl = blob.signUrl(7, TimeUnit.DAYS).toString();

            System.out.println("✅ File uploaded to Firebase: " + fileName);
            return signedUrl;

        } catch (Exception e) {
            System.err.println("Firebase upload failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Upload file from bytes (for AI service integration)
     */
    public String uploadBytes(byte[] fileBytes, String fileName, String contentType, String folder, Long userId) throws IOException {
        if (!firebaseEnabled || bucket == null) {
            return null;
        }

        try {
            String fullPath = folder + "/" + userId + "_" + System.currentTimeMillis() + "_" + fileName;
            Blob blob = bucket.create(fullPath, fileBytes, contentType);
            String signedUrl = blob.signUrl(7, TimeUnit.DAYS).toString();
            System.out.println("✅ Bytes uploaded to Firebase: " + fullPath);
            return signedUrl;

        } catch (Exception e) {
            System.err.println("Firebase bytes upload failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete file from Firebase Storage
     */
    public boolean deleteFile(String fileUrl) {
        if (!firebaseEnabled || bucket == null || fileUrl == null) {
            return false;
        }

        try {
            // Extract blob name from URL
            String blobName = extractBlobNameFromUrl(fileUrl);
            if (blobName != null) {
                Blob blob = bucket.get(blobName);
                if (blob != null) {
                    blob.delete();
                    System.out.println("✅ File deleted from Firebase: " + blobName);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Firebase delete failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get download URL for existing file
     */
    public String getDownloadUrl(String blobName) {
        if (!firebaseEnabled || bucket == null) {
            return null;
        }

        try {
            Blob blob = bucket.get(blobName);
            if (blob != null) {
                return blob.signUrl(7, TimeUnit.DAYS).toString();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to get download URL: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract blob name from Firebase URL
     */
    private String extractBlobNameFromUrl(String url) {
        try {
            // Firebase URL format: https://storage.googleapis.com/bucket-name/folder/file.jpg
            if (url.contains("/o/")) {
                String[] parts = url.split("/o/");
                if (parts.length > 1) {
                    String encoded = parts[1].split("\\?")[0];
                    return java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isEnabled() {
        return firebaseEnabled;
    }
}
