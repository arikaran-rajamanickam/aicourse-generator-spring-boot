package com.aicourse.mcp.transport;

import com.aicourse.mcp.service.McpFacadeService;
import com.aicourse.mcp.service.McpToolRegistry;
import com.auth.enums.UserRole;
import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = McpController.class)
@Import(com.aicourse.config.Config.class)
@TestPropertySource(properties = "mcp.enabled=false")
class McpControllerDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private McpFacadeService mcpFacadeService;

    @MockitoBean
    private McpToolRegistry mcpToolRegistry;

    @MockitoBean
    private com.auth.service.UserDetailService userDetailService;

    @MockitoBean
    private com.auth.filter.JWTFilter jwtFilter;

    @MockitoBean
    private com.auth.jwt.impl.AuthenticationSuccessHandlerImpl authenticationSuccessHandler;

    @BeforeEach
    void allowFilterChainToProceed() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void executeReturnsNotFoundWhenMcpDisabledEvenForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/mcp/execute")
                        .with(authentication(adminAuthentication()))
                        .contentType("application/json")
                        .content("{\"tool\":\"lesson.generate\",\"input\":{}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("MCP is disabled"));

        verifyNoInteractions(mcpFacadeService);
    }

    private Authentication adminAuthentication() {
        Users user = new Users();
        user.setId(1L);
        user.setUsername("admin");
        user.setRoles(UserRole.ADMIN);
        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}



