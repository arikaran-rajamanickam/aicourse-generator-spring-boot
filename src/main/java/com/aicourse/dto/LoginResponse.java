package com.aicourse.dto;

@Deprecated
public class LoginResponse extends com.auth.dto.LoginResponse {
    public LoginResponse(String token, com.auth.dto.UserResponse user) {
        super(token, user);
    }
}
