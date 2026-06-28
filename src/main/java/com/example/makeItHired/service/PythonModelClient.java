
package com.example.makeItHired.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;


import java.io.File;
import java.util.*;

@Service
public class PythonModelClient {

    private final WebClient webClient;

    @Value("${models.resume.parse.url:http://localhost:8000/parse-resume}")
    private String resumeParseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PythonModelClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder().build();
    }

    private String getPythonServiceUrl() {
        if (resumeParseUrl != null && !resumeParseUrl.isEmpty()) {
            try {
                java.net.URI uri = new java.net.URI(resumeParseUrl);
                String scheme = uri.getScheme();
                String authority = uri.getAuthority();
                if (scheme != null && authority != null) {
                    return scheme + "://" + authority;
                }
            } catch (Exception e) {
                System.err.println("Error parsing resumeParseUrl: " + e.getMessage());
            }
            return resumeParseUrl;
        }
        return "http://localhost:8000";
    }


    public String parseResume(File file) {
        try {
            System.out.println("=== PYTHON MODEL CLIENT ===");
            System.out.println("Sending file to Python service: " + file.getAbsolutePath());
            String pythonUrl = getPythonServiceUrl();
            System.out.println("Python service URL: " + pythonUrl + "/parse-resume");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String url = pythonUrl + "/parse-resume";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            System.out.println("Python service response status: " + response.getStatusCode());
            System.out.println("Python service response body: " + response.getBody());

            String responseBody = response.getBody();
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                try {
                    Map<String, Object> map = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                    if (map != null) {
                        Object scoreObj = map.get("resume_score");
                        if (scoreObj == null) {
                            map.put("resume_score", 50);
                            map.put("warning", "Resume score was missing from AI response; using fallback.");
                            if (!map.containsKey("score_breakdown")) {
                                Map<String, Object> breakdown = new HashMap<>();
                                breakdown.put("contact_info", 10);
                                breakdown.put("education", 10);
                                breakdown.put("experience", 10);
                                breakdown.put("skills", 10);
                                breakdown.put("formatting_length", 10);
                                map.put("score_breakdown", breakdown);
                            }
                            responseBody = objectMapper.writeValueAsString(map);
                        } else {
                            int score = 0;
                            if (scoreObj instanceof Number) {
                                score = ((Number) scoreObj).intValue();
                            } else {
                                try {
                                    score = Integer.parseInt(scoreObj.toString());
                                } catch (Exception parseEx) {}
                            }
                            if (score <= 0) {
                                map.put("resume_score", 50);
                                map.put("warning", "Resume score calculated as 0; using fallback default.");
                                if (!map.containsKey("score_breakdown")) {
                                    Map<String, Object> breakdown = new HashMap<>();
                                    breakdown.put("contact_info", 10);
                                    breakdown.put("education", 10);
                                    breakdown.put("experience", 10);
                                    breakdown.put("skills", 10);
                                    breakdown.put("formatting_length", 10);
                                    map.put("score_breakdown", breakdown);
                                }
                                responseBody = objectMapper.writeValueAsString(map);
                            }
                        }
                    }
                } catch (Exception parseEx) {
                    System.err.println("Warning: Python service response body is not valid JSON. Generating local fallback.");
                    return generateFallbackJson(file, "AI response was not valid JSON: " + parseEx.getMessage());
                }
            } else {
                return generateFallbackJson(file, "Empty response from AI service.");
            }
            return responseBody;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("HTTP Error from Python service: " + e.getStatusCode());
            System.err.println("Response body: " + e.getResponseBodyAsString());
            e.printStackTrace();
            return generateFallbackJson(file, "HTTP " + e.getStatusCode() + ": " + e.getStatusText());
        } catch (Exception e) {
            System.err.println("Error calling Python service: " + e.getMessage());
            e.printStackTrace();
            return generateFallbackJson(file, e.getMessage());
        }
    }

    private String generateFallbackJson(File file, String errorMsg) {
        try {
            String filename = file != null ? file.getName() : "resume.pdf";
            long fileSize = file != null ? file.length() : 0;
            
            int fallbackScore = 55;
            String nameLower = filename.toLowerCase();
            if (nameLower.contains("resume") || nameLower.contains("cv")) {
                fallbackScore += 10;
            }
            if (fileSize > 50000) {
                fallbackScore += 15;
            }
            fallbackScore = Math.min(85, fallbackScore);
            
            int contact = (int) (fallbackScore * 0.20);
            int education = (int) (fallbackScore * 0.25);
            int experience = (int) (fallbackScore * 0.20);
            int skills = (int) (fallbackScore * 0.20);
            int formatting = fallbackScore - (contact + education + experience + skills);
            
            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("contact_info", contact);
            breakdown.put("education", education);
            breakdown.put("experience", experience);
            breakdown.put("skills", skills);
            breakdown.put("formatting_length", formatting);
            
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("session_id", "fallback-" + UUID.randomUUID().toString());
            fallback.put("resume_score", fallbackScore);
            fallback.put("score_breakdown", breakdown);
            fallback.put("skills_found", Arrays.asList("Communication", "Problem Solving"));
            fallback.put("experience_years", 1.0);
            fallback.put("recommendations", Arrays.asList(
                "AI Parsing Service is currently offline or encountered an error. Calculated local fallback score based on file properties.",
                "Verify connection to the AI microservice for in-depth keyword analysis."
            ));
            fallback.put("word_count", (int)(fileSize / 150));
            fallback.put("filename", filename);
            fallback.put("warning", "Local Fallback Score: AI service unreachable (" + errorMsg + ").");
            fallback.put("is_fallback", true);
            
            return objectMapper.writeValueAsString(fallback);
        } catch (Exception e) {
            return "{\"resume_score\":60,\"score_breakdown\":{\"contact_info\":12,\"education\":15,\"experience\":12,\"skills\":12,\"formatting_length\":9},\"warning\":\"Local Fallback Mode Activated\"}";
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

            String url = getPythonServiceUrl() + "/match-job";
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

            String url = getPythonServiceUrl() + "/verify-identity";
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

            String url = getPythonServiceUrl() + "/analyze-video";
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

            String url = getPythonServiceUrl() + "/generate-questions";
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

            String url = getPythonServiceUrl() + "/evaluate-answer";
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

            String url = getPythonServiceUrl() + "/generate-report";
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
            String url = getPythonServiceUrl() + "/reports";
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
            String url = getPythonServiceUrl() + "/report/" + reportId;
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
