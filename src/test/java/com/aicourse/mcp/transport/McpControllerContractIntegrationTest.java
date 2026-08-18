package com.aicourse.mcp.transport;

import com.aicourse.mcp.dto.McpToolResponse;
import com.aicourse.mcp.service.McpFacadeService;
import com.aicourse.mcp.service.McpToolRegistry;
import com.auth.enums.UserRole;
import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = McpController.class)
@Import(com.aicourse.config.Config.class)
@TestPropertySource(properties = "mcp.enabled=true")
class McpControllerContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void executeSuccessResponseShapeIsStable() throws Exception {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("lessonId", 3001L);
        data.put("enriched", true);

        when(mcpFacadeService.execute(any(), any()))
                .thenReturn(McpToolResponse.success("lesson.generate", data));

        mockMvc.perform(post("/api/mcp/execute")
                        .with(authentication(adminAuthentication()))
                        .contentType("application/json")
                        .content("{\"tool\":\"lesson.generate\",\"input\":{\"courseId\":1001,\"moduleId\":2001,\"lessonId\":3001}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tool").value("lesson.generate"))
                .andExpect(jsonPath("$.data.lessonId").value(3001))
                .andExpect(jsonPath("$.data.enriched").value(true));
    }

    @Test
    void executeFailureResponseShapeIsStable() throws Exception {
        when(mcpFacadeService.execute(any(), any()))
                .thenReturn(McpToolResponse.failure("llm.routes.upsert", "Admin access required"));

        mockMvc.perform(post("/api/mcp/execute")
                        .with(authentication(adminAuthentication()))
                        .contentType("application/json")
                        .content("{\"tool\":\"llm.routes.upsert\",\"input\":{\"workload\":\"AI_COACH\",\"providerCode\":\"gemini\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.tool").value("llm.routes.upsert"))
                .andExpect(jsonPath("$.error").value("Admin access required"));
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

