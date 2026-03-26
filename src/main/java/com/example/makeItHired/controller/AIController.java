package com.example.makeItHired.controller;

import com.example.makeItHired.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/resume")
    public String analyzeResume(@RequestParam("file")MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("resume", ".pdf");
        file.transferTo(tempFile);

        return aiService.analyzeResume(tempFile);
    }
}
