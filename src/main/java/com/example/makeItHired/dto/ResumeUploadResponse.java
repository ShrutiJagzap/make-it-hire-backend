package com.example.makeItHired.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeUploadResponse {
    private Long resumeId;
    private String parsedData;

    public ResumeUploadResponse(Long resumeId, String parsedData) {
        this.resumeId = resumeId;
        this.parsedData = parsedData;
    }
}
