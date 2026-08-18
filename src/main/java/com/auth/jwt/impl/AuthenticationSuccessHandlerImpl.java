package com.auth.jwt.impl;

import com.auth.enums.UserRole;
import com.auth.jwt.JWTService;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserRepo userRepo;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        String email = null;
        if (authentication.getPrincipal() instanceof OAuth2User oauthUser) {
            email = oauthUser.getAttribute("email");
        }

        if (email == null) {
            email = authentication.getName(); // Fallback
        }

        System.out.println("OAuth2 Login: Processing email: " + email);

        Users user = userRepo.findByUsername(email);
        if (user == null) {
            System.out.println("OAuth2 Login: User not found, creating new user for: " + email);
            user = new Users();
            user.setUsername(email);
            user.setPassword(""); // No password for OAuth users
            user.setRoles(UserRole.USER);
            userRepo.save(user);
        } else {
            System.out.println("OAuth2 Login: User found: " + user.getUsername());
        }

        String token = jwtService.generateToken(user.getUsername());
        System.out.println("Generated Token for " + user.getUsername() + ": " + token);

        String normalizedFrontendBaseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;

        String targetUrl = UriComponentsBuilder.fromUriString(normalizedFrontendBaseUrl)
                .path("/oauth-success")
                .queryParam("token", token)
                .build().toUriString();
        System.out.println("Redirecting to: " + targetUrl);

        response.sendRedirect(targetUrl);
    }
}

