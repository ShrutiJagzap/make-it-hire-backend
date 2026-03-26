
package com.example.makeItHired.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;


import java.io.File;
import java.util.*;

@Service
public class PythonModelClient {

    private final String PYTHON_SERVICE_URL = "http://localhost:8000"; // Change to your Python server IP
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PythonModelClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String parseResume(File file) {
        try {
            System.out.println("=== PYTHON MODEL CLIENT ===");
            System.out.println("Sending file to Python service: " + file.getAbsolutePath());
            System.out.println("Python service URL: " + PYTHON_SERVICE_URL + "/parse-resume");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String url = PYTHON_SERVICE_URL + "/parse-resume";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            System.out.println("Python service response status: " + response.getStatusCode());
            System.out.println("Python service response body: " + response.getBody());

            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("HTTP Error from Python service: " + e.getStatusCode());
            System.err.println("Response body: " + e.getResponseBodyAsString());
            e.printStackTrace();
            try {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "AI service error");
                error.put("status", e.getStatusCode().value());
                error.put("message", e.getResponseBodyAsString());
                error.put("resume_score", 0);
                error.put("skills_found", new ArrayList<>());
                return objectMapper.writeValueAsString(error);
            } catch (Exception ex) {
                return "{\"error\":\"Failed to analyze resume\",\"resume_score\":0}";
            }
        } catch (Exception e) {
            System.err.println("Error calling Python service: " + e.getMessage());
            e.printStackTrace();
            try {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to analyze resume");
                error.put("message", e.getMessage());
                error.put("resume_score", 0);
                error.put("skills_found", new ArrayList<>());
                return objectMapper.writeValueAsString(error);
            } catch (Exception ex) {
                return "{\"error\":\"Failed to analyze resume\", \"resume_score\":0}";
            }
        }
    }

    public Map<String, Object> matchResumeWithJob(String sessionId, String jobDescription) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            if (sessionId != null) {
                body.add("session_id", sessionId);
            }
            body.add("job_description", jobDescription);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            String url = PYTHON_SERVICE_URL + "/match-job";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to match resume with job");
            error.put("message", e.getMessage());
            return error;
        }
    }

    public Map<String, Object> verifyIdentity(String sessionId, String base64Image) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("session_id", sessionId);
            body.add("image", base64Image);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            String url = PYTHON_SERVICE_URL + "/verify-identity";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("verified", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    public Map<String, Object> analyzeVideoFrame(String sessionId, String base64Frame) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("session_id", sessionId);
            body.add("frame", base64Frame);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            String url = PYTHON_SERVICE_URL + "/analyze-video";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("emotion", "neutral");
            error.put("engagement_score", 0);
            error.put("status", "Error");
            return error;
        }
    }

    public Map<String, Object> generateQuestions(String sessionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("session_id", sessionId);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            String url = PYTHON_SERVICE_URL + "/generate-questions";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to generate questions");
            return error;
        }
    }

    public Map<String, Object> evaluateAnswer(String sessionId, String answer, int questionIndex) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("session_id", sessionId);
            body.add("answer", answer);
            body.add("question_index", String.valueOf(questionIndex));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String url = PYTHON_SERVICE_URL + "/evaluate-answer";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("score", 0);
            error.put("feedback", "Evaluation failed: " + e.getMessage());
            return error;
        }
    }

    public Map<String, Object> generateReport(String sessionId, String candidateName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("session_id", sessionId);
            body.add("candidate_name", candidateName);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            String url = PYTHON_SERVICE_URL + "/generate-report";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to generate report");
            return error;
        }
    }

    public List<Map<String, Object>> getAllReports() {
        try {
            String url = PYTHON_SERVICE_URL + "/reports";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getReport(String reportId) {
        try {
            String url = PYTHON_SERVICE_URL + "/report/" + reportId;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Report not found");
            return error;
        }
    }
}
