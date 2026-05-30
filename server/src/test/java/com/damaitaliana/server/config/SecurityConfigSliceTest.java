package com.damaitaliana.server.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.damaitaliana.server.auth.AuthController;
import com.damaitaliana.server.auth.JwtAuthenticationEntryPoint;
import com.damaitaliana.server.auth.JwtAuthenticationFilter;
import com.damaitaliana.server.auth.JwtClaims;
import com.damaitaliana.server.auth.JwtService;
import com.damaitaliana.server.auth.JwtVerificationException;
import com.damaitaliana.server.entity.Role;
import com.damaitaliana.server.user.UserController;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slice test for {@link SecurityConfig} + {@link JwtAuthenticationFilter} + {@link
 * JwtAuthenticationEntryPoint}. Verifies the four behaviours the security chain promises:
 * whitelisted paths reach the controller without authentication, secured paths reject anonymous
 * requests, valid Bearer tokens are accepted, invalid Bearer tokens are rejected with {@code
 * application/problem+json}.
 */
@WebMvcTest(
    excludeFilters =
        @Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {AuthController.class, UserController.class}))
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  JwtAuthenticationEntryPoint.class,
  SecurityConfigSliceTest.SecuredController.class,
  SecurityConfigSliceTest.WhitelistedAuthController.class
})
class SecurityConfigSliceTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private JwtService jwtService;

  @Test
  void securedEndpoint_returns401Problem_whenAuthorizationHeaderMissing() throws Exception {
    mockMvc
        .perform(get("/test/secured"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Authentication required"));
  }

  @Test
  void securedEndpoint_reachesController_whenBearerTokenVerifies() throws Exception {
    JwtClaims claims =
        new JwtClaims(
            42L, "alice", Role.USER, JwtClaims.TokenType.ACCESS, Instant.now().plusSeconds(60));
    when(jwtService.parse(any())).thenReturn(claims);

    mockMvc
        .perform(get("/test/secured").header("Authorization", "Bearer good-token"))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
  }

  @Test
  void securedEndpoint_returns401Problem_whenBearerTokenFailsVerification() throws Exception {
    when(jwtService.parse(any())).thenThrow(new JwtVerificationException("invalid for test"));

    mockMvc
        .perform(get("/test/secured").header("Authorization", "Bearer bad-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Unauthorized"));
  }

  @Test
  void whitelistedAuthEndpoint_reachesController_withoutAuthentication() throws Exception {
    mockMvc.perform(get("/v1/auth/_test_whitelisted")).andExpect(status().isOk());
  }

  @Test
  void corsPreflight_isAllowed_evenWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            options("/test/secured")
                .header("Origin", "http://localhost:8080")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk());
  }

  @RestController
  static class SecuredController {
    @GetMapping("/test/secured")
    String secured() {
      return "ok";
    }
  }

  @RestController
  static class WhitelistedAuthController {
    @GetMapping("/v1/auth/_test_whitelisted")
    String whitelisted() {
      return "ok";
    }
  }
}
