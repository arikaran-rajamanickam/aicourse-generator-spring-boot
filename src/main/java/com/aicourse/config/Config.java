package com.aicourse.config;

// ... keeping your existing imports ...

import com.auth.filter.JWTFilter;
import com.auth.jwt.RevocationLogoutHandler;
import com.auth.jwt.impl.AuthenticationSuccessHandlerImpl;
import com.auth.service.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class Config {

        @Autowired
        private UserDetailService userDetailsService;

        @Autowired
        private JWTFilter jwtFilter;

        @Autowired
        private AuthenticationSuccessHandlerImpl authenticationSuccessHandler;

        // New: revoke tokens on logout
        @Autowired
        private RevocationLogoutHandler revocationLogoutHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .csrf(csrf -> csrf.disable())

                                // 🔹 ADD THIS LINE FOR CORS
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                .oauth2Login(oauth -> oauth
                                                .successHandler(authenticationSuccessHandler))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(
                                                                "/api/auth/**",
                                                                "/api/content/public/**",
                                                                "/api/join/**",
                                                                "/login",
                                                                "/register",
                                                                "/oauth2/**",
                                                                "/error")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/leaderboard/global").permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .logout(logout -> logout
                                                .logoutUrl("/api/auth/logout")
                                                .addLogoutHandler(revocationLogoutHandler)
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID")
                                                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200)) // Return
                                                                                                              // 200 OK
                                )
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        // 🔹 ADD THIS BEAN
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // Allow frontend origins explicitly. Avoid wildcard when allowCredentials=true.
                configuration.setAllowedOriginPatterns(List.of(
                                "http://localhost:5173",
                                "http://127.0.0.1:5173",
                                "http://localhost:3000",
                                "http://127.0.0.1:5174",
                                "http://192.168.*.*:5173",
                                "http://10.*.*.*:5173"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(
                                List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        // Register a CorsFilter with highest precedence to ensure CORS checks run
        // before security filters.
        @Bean
        public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
                UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource();
                FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
                bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
                return bean;
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setPasswordEncoder(new BCryptPasswordEncoder(12));
                authProvider.setUserDetailsService(userDetailsService);
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        @ConditionalOnMissingBean(org.springframework.security.oauth2.client.registration.ClientRegistrationRepository.class)
        public org.springframework.security.oauth2.client.registration.ClientRegistrationRepository dummyClientRegistrationRepository() {
                return new org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository(
                                org.springframework.security.oauth2.client.registration.ClientRegistration
                                                .withRegistrationId("google")
                                                .authorizationGrantType(
                                                                org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                                                .clientId("missing-client-id")
                                                .clientSecret("missing-client-secret")
                                                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                                                .tokenUri("https://oauth2.googleapis.com/token")
                                                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                                                .clientName("Google")
                                                .build());
        }
}