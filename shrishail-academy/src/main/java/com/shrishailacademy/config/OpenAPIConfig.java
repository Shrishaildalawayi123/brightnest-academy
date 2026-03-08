package com.shrishailacademy.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration for BrightNest Academy API
 * 
 * Automatically generates interactive API documentation at:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 * - OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "BrightNest Academy API",
        version = "1.0.0",
        description = """
            RESTful API for BrightNest Academy - Multi-tenant Education Platform
            
            ## Features
            - Multi-tenant architecture with tenant isolation
            - JWT-based authentication (Bearer token)
            - Role-based access control (ADMIN, TEACHER, STUDENT)
            - Course enrollment and payment management
            - Contact forms, demo bookings, and teacher applications
            - Admin dashboard with analytics
            - Rate limiting and security hardening
            
            ## Authentication
            1. Register/Login via `/api/auth/register` or `/api/auth/login`
            2. Use returned JWT token in `Authorization: Bearer <token>` header
            3. Refresh tokens via `/api/auth/refresh` before expiry
            
            ## Multi-Tenancy
            - API supports multiple tenants via `X-Tenant-Key` header (defaults to 'default')
            - Tenant isolation enforced at database and service layers
            """,
        contact = @Contact(
            name = "BrightNest Academy",
            email = "admin@brightnest-academy.com",
            url = "https://brightnest-academy.com"
        ),
        license = @License(
            name = "Proprietary",
            url = "https://brightnest-academy.com/license"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Development Server"
        ),
        @Server(
            url = "https://api.brightnest-academy.com",
            description = "Production Server"
        )
    },
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT authentication. Login via /api/auth/login to obtain token."
)
public class OpenAPIConfig {
    // Configuration via annotations - no additional code required
    // SpringDoc auto-detects @RestController endpoints and generates docs
}
