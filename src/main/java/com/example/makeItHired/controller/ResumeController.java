package com.example.makeItHired.controller;

import com.example.makeItHired.dto.ResumeUploadResponse;
import com.example.makeItHired.entity.Resume;
import com.example.makeItHired.entity.User;
import com.example.makeItHired.repository.ResumeRepository;
import com.example.makeItHired.repository.UserRepository;
import com.example.makeItHired.service.FileStorageService;
import com.example.makeItHired.service.PythonModelClient;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final FileStorageService fileStorage;
    private final ResumeRepository resumeRepo;
    private final UserRepository userRepo;
    private final PythonModelClient modelClient;
    private final ObjectMapper objectMapper;

    public ResumeController(FileStorageService fileStorage, ResumeRepository resumeRepo, UserRepository userRepo, PythonModelClient modelClient) {
        this.fileStorage = fileStorage;
        this.resumeRepo = resumeRepo;
        this.userRepo = userRepo;
        this.modelClient = modelClient;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId) {
        try {
            System.out.println("=== RESUME UPLOAD STARTED ===");
            System.out.println("Filename: " + file.getOriginalFilename());
            System.out.println("Size: " + file.getSize());
            System.out.println("UserID: " + userId);
            //validate user
            if(userId == null) {
                System.out.println("ERROR: User ID is null");
                return ResponseEntity.badRequest().body(Map.of("message", "User ID is required"));
            }
            Optional<User> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                System.out.println("ERROR: User not found with ID: " + userId);
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            //1. Store file
            Path stored = fileStorage.storeFile(file,"resumes");
            System.out.println("File stored at: " + stored.toString());

            //2. Save resume metadata
            Resume resume = new Resume();
            resume.setFilename(file.getOriginalFilename());
            resume.setFilepath(stored.toString());
            resume.setUserId(userId);
            resume.setUploadedAt(LocalDateTime.now());
            resume = resumeRepo.save(resume);
            System.out.println("Resume saved with ID: " + resume.getId());

            //3. Call Python AI model for comprehensive analysis
            String aiResult = modelClient.parseResume(stored.toFile());
            System.out.println("Resume saved with ID: " + resume.getId());

            //parse the result to ensure it's valid JSON
            try{
                objectMapper.readTree(aiResult);
                resume.setParsedJson(aiResult);
            } catch (Exception e) {
                //If not valid JSON wrap it
                Map<String, String> wrapped = Map.of("result", aiResult);
                resume.setParsedJson(objectMapper.writeValueAsString(wrapped));
            }
            resumeRepo.save(resume);
            System.out.println("=== RESUME UPLOAD COMPLETED SUCCESSFULLY ===");
            return ResponseEntity.ok(new ResumeUploadResponse(resume.getId(), resume.getParsedJson()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Upload failed:" + e.getMessage()));
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
