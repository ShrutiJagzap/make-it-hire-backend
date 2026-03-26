package com.example.makeItHired.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Data
public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(){}

}
