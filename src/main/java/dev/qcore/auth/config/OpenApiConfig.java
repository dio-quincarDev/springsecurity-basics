package dev.qcore.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth API")
                        .version("1.0.0")
                        .description("API de autenticación y autorización con Spring Security + JWT.\n\n" +
                                "Maneja registro de usuarios, inicio de sesión, " +
                                "validación de tokens y control de acceso por roles (USER / ADMIN).")
                        .contact(new Contact()
                                .name("Diogenes Quintero")
                                .email("dio-quincar@outlook.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT en formato: Bearer &lt;token&gt;")));
    }
}