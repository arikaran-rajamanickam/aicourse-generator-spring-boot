package com.auth.service;

import com.aicourse.utils.exception.AuthenticationFailedException;
import com.aicourse.utils.id.SnowflakeIdGenerator;
import com.auth.dto.LoginResponse;
import com.auth.dto.UserResponse;
import com.auth.jwt.JWTService;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UserService {

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    private final static BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JWTService jwtService;
    @Autowired
    private com.search.service.SearchService searchService;

    public static BCryptPasswordEncoder getEncoder() {
        return encoder;
    }

    public Users registerUser(Users user) {
        LOGGER.log(Level.INFO, "Attempting to register new user: {0}", new Object[]{user.getUsername()});
        try {
            String handle = user.getUsername() == null ? null : user.getUsername().trim();
            if (handle == null || handle.isBlank()) {
                throw new IllegalArgumentException("User ID is required.");
            }
            user.setUsername(handle);

            String displayName = user.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                user.setDisplayName(handle);
            } else {
                user.setDisplayName(displayName.trim());
            }

            user.setId(SnowflakeIdGenerator.generateId());
            user.setPassword(encoder.encode(user.getPassword()));
            // Note: roles and timestamps are now handled automatically in Users.java
            // @PrePersist
            Users savedUser = userRepo.save(user);
            LOGGER.log(Level.INFO, "User registered successfully with ID: {0}", new Object[]{savedUser.getId()});

            // Refresh search indices so new user can be found
            try {
                searchService.refreshAllIndices();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to refresh search indices: {0}", e.getMessage());
            }
            
            return savedUser;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Registration failed for user: {0}: {1}",
                    new Object[]{user.getUsername(), e.getMessage()});
            throw e;
        }
    }

    public LoginResponse issueLoginResponse(Users user) {
        String token = jwtService.generateToken(user.getUsername());
        return new LoginResponse(token, new UserResponse(user));
    }

    public LoginResponse verify(Users user) {
        LOGGER.log(Level.INFO, "Attempting to verify user: {0}", new Object[]{user.getUsername()});
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

            if (authentication.isAuthenticated()) {
                LOGGER.log(Level.INFO, "Authentication successful for user: {0}", new Object[]{user.getUsername()});

                // Set the SecurityContext for the current thread
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Persist SecurityContext into HTTP session so subsequent requests with the same
                // JSESSIONID are treated as authenticated for this principal.
                try {
                    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attrs != null) {
                        HttpServletRequest currentRequest = attrs.getRequest();
                        if (currentRequest != null) {
                            HttpSession session = currentRequest.getSession(true);
                            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                    SecurityContextHolder.getContext());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Unable to persist security context to session: {0}", new Object[]{e.getMessage()});
                }

                String token = jwtService.generateToken(user.getUsername());
                Users currentUser = userRepo.findByUsername(user.getUsername());
                return new LoginResponse(token, new UserResponse(currentUser));
            } else {
                LOGGER.log(Level.WARNING, "Authentication failed for user: {0}", new Object[]{user.getUsername()});
                throw new AuthenticationFailedException("User is not verified");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login process interrupted for user: {0}: {1}",
                    new Object[]{user.getUsername(), e.getMessage()});
            throw e;
        }
    }
}
