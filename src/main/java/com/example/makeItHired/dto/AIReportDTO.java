package com.example.makeItHired.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Data
@Getter
@Setter
public class AIReportDTO {
    private String candidateName;
    private String date;
    private double resumeScore;
    private double matchScore;
    private boolean identityVerified;
    private double identityConfidence;
    private double interviewScore;
    private double engagementScore;
    private String dominantEmotion;
    private List<String> skillsFound;
    private List<Map<String, Object>> answers;
    private Map<String, Object> behaviorSummary;
    private double overallScore;
    private String verdict;
    private List<String> recommendations;
}
