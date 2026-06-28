package com.example.makeItHired.controller;

import com.example.makeItHired.dto.ResumeUploadResponse;
import com.example.makeItHired.entity.Resume;
import com.example.makeItHired.entity.User;
import com.example.makeItHired.repository.ResumeRepository;
import com.example.makeItHired.repository.UserRepository;
import com.example.makeItHired.service.FileStorageService;
import com.example.makeItHired.service.FirebaseStorageService;
import com.example.makeItHired.service.PythonModelClient;
import com.example.makeItHired.entity.Role;
import com.example.makeItHired.service.NotificationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeController {

    @Autowired
    private FirebaseStorageService firebaseStorageService;
    private final FileStorageService fileStorage;
    private final ResumeRepository resumeRepo;
    private final UserRepository userRepo;
    private final PythonModelClient modelClient;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public ResumeController(FileStorageService fileStorage, ResumeRepository resumeRepo, UserRepository userRepo, PythonModelClient modelClient, NotificationService notificationService) {
        this.fileStorage = fileStorage;
        this.resumeRepo = resumeRepo;
        this.userRepo = userRepo;
        this.modelClient = modelClient;
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId) {
        try {
            System.out.println("=== RESUME UPLOAD STARTED ===");

            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User ID is required"));
            }

            Optional<User> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            String tempDir = System.getProperty("java.io.tmpdir");
//            File tempFile = File.createTempFile("resume_", ".pdf");
            File tempFile = new File(tempDir, "resume_" + System.currentTimeMillis() + ".pdf");
            file.transferTo(tempFile);
            System.out.println("Temp file created: " + tempFile.getAbsolutePath());

            Resume resume = new Resume();
            resume.setFilename(file.getOriginalFilename());


            resume.setUserId(userId);
            resume.setUploadedAt(LocalDateTime.now());
            resume = resumeRepo.save(resume);

            // Call Python AI model
            String aiResult = modelClient.parseResume(tempFile);
            resume.setParsedJson(aiResult);
            resume = resumeRepo.save(resume);

            // Trigger notifications
            try {
                User user = userOpt.get();
                // Send candidate notification
                notificationService.createNotification(
                    userId,
                    null,
                    "Resume Uploaded Successfully!",
                    "Your resume \"" + file.getOriginalFilename() + "\" was uploaded and analyzed.",
                    "FEEDBACK",
                    "/user-dashboard"
                );
                // Send HR/Admin notification
                notificationService.createNotification(
                    null,
                    Role.ADMIN,
                    "New Candidate Application",
                    user.getFullName() + " uploaded a new resume.",
                    "APPLICATION",
                    "/admin-dashboard"
                );
            } catch (Exception notifEx) {
                System.err.println("Failed to trigger upload notifications: " + notifEx.getMessage());
            }

            return ResponseEntity.ok(new ResumeUploadResponse(resume.getId(), resume.getParsedJson()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/match-job")
    public ResponseEntity<?> matchResumeWithJob(
            @RequestParam("resumeId") Long resumeId,
            @RequestParam("jobDescription") String jobDescription) {
        try {
            Optional<Resume> resumeOpt = resumeRepo.findById(resumeId);
            if (resumeOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Resume resume = resumeOpt.get();

            //Extract session ID from parse JSON if available
            String parsedJson = resume.getParsedJson();
            String sessionId = null;

            if (parsedJson != null && !parsedJson.isEmpty()) {
                try{
                    var jsonNode = objectMapper.readTree(parsedJson);
                    if (jsonNode.has("session_id")) {
                        sessionId = jsonNode.get("session_id").asText();
                    }
                } catch (Exception e) {}
            }
            //Call AI service to match with job
            Map<String,Object> result = modelClient.matchResumeWithJob(sessionId, jobDescription);

            // Trigger match notification
            try {
                notificationService.createNotification(
                    resume.getUserId(),
                    null,
                    "Resume Match Complete!",
                    "Your matching result for job description is complete.",
                    "FEEDBACK",
                    "/user-dashboard"
                );
            } catch (Exception notifEx) {
                System.err.println("Failed to trigger match notification: " + notifEx.getMessage());
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Maching failed:" + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserResumes(@PathVariable Long userId) {
        try {
            List<Resume> resumes = resumeRepo.findByUserId(userId);

            // Parse JSON for each resume to make it frontend-ready
            for (Resume resume : resumes) {
                if (resume.getParsedJson() != null && !resume.getParsedJson().isEmpty()) {
                    try {
                        // Ensure it's valid JSON
                        objectMapper.readTree(resume.getParsedJson());
                    } catch (Exception e) {
                        // If not valid, set to null
                        resume.setParsedJson(null);
                    }
                }
            }

            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to fetch resumes"));
        }
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<?> getResume(@PathVariable Long resumeId) {
        Optional<Resume> resume = resumeRepo.findById(resumeId);
        if (resume.isPresent()) {
            return ResponseEntity.ok(resume.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/analyze/{resumeId}")
    public ResponseEntity<?> analyzeResumeInDepth(@PathVariable Long resumeId) {
        try {
            Optional<Resume> resumeOpt = resumeRepo.findById(resumeId);
            if (resumeOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Resume resume = resumeOpt.get();

            // If we have parsed JSON, return it
            if (resume.getParsedJson() != null && !resume.getParsedJson().isEmpty()) {
                try {
                    var jsonNode = objectMapper.readTree(resume.getParsedJson());
                    return ResponseEntity.ok(jsonNode);
                } catch (Exception e) {
                    // Continue to re-analyze
                }
            }

            // Otherwise, re-analyze the file
            if (resume.getFilepath() != null) {
                File file = new File(resume.getFilepath());
                if (file.exists()) {
                    String aiResult = modelClient.parseResume(file);
                    resume.setParsedJson(aiResult);
                    resumeRepo.save(resume);

                    var jsonNode = objectMapper.readTree(aiResult);
                    return ResponseEntity.ok(jsonNode);
                }
            }

            return ResponseEntity.ok(Map.of("message", "No analysis available"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Analysis failed: " + e.getMessage()));
        }
    }

    @GetMapping("/reports/all")
    public ResponseEntity<?> getAllReports() {
        try {
            List<Map<String, Object>> reports = modelClient.getAllReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to fetch reports"));
        }
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<?> getReport(@PathVariable String reportId) {
        try {
            Map<String, Object> report = modelClient.getReport(reportId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to fetch report"));
        }
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<?> deleteResume(@PathVariable Long resumeId) {
        try {
            resumeRepo.deleteById(resumeId);
            return ResponseEntity.ok(Map.of("message", "Resume deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to delete resume"));
        }
    }

    private Long getUserIdFromPrinciple(Principal principal) {
        return 1L;
    }

}
