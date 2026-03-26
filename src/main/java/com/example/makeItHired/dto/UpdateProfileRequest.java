package com.example.makeItHired.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class UpdateProfileRequest {
    private String fullName;
    private String email;
    private String phone;
    private String title;


}
