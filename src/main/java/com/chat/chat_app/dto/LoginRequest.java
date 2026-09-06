package com.chat.chat_app.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}