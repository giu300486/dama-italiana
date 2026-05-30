package com.damaitaliana.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.1 document metadata and the shared {@code bearerAuth} security scheme (NFR-M-03,
 * springdoc 2.6).
 *
 * <p>The actual operation/response schemas are contributed by the {@code @Operation} /
 * {@code @ApiResponses} annotations on the controllers; this bean only supplies the document-level
 * {@code info} block and registers the HTTP Bearer (JWT) scheme so the Swagger UI grows an
 * "Authorize" button. Endpoints opt into it with {@code @SecurityRequirement(name = "bearerAuth")}
 * — the four {@code /v1/auth/**} entry points stay unsecured (they mint or exchange tokens), while
 * {@code GET /v1/users/me} and the token-carrying {@code /refresh} + {@code /logout} reference it.
 *
 * <p>The 3.1 output (versus springdoc's 3.0 default) is selected via {@code
 * springdoc.api-docs.version=openapi_3_1} in {@code application.yml}.
 */
@Configuration
public class OpenApiConfig {

  /** Referenced by name from the controllers' {@code @SecurityRequirement} annotations. */
  public static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI damaOpenApi(@Value("${dama.version:0.5.0}") String version) {
    return new OpenAPI()
        .info(
            new Info()
                .title("Dama Italiana Server API")
                .version(version)
                .description(
                    "Authentication and user-profile endpoints for the Dama Italiana central"
                        + " server: registration, login, JWT access/refresh rotation, logout, and"
                        + " the caller's own profile."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "JWT access token (or refresh token for /refresh + /logout).")));
  }
}
