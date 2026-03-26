package com.example.makeItHired.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String role;

    public RegisterRequest() {}
}
