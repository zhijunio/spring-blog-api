package com.example.blog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI openApi(OpenApiProperties openApiProps) {
        Contact contact = new Contact()
                .name(openApiProps.contact().name())
                .email(openApiProps.contact().email());
        Info info = new Info()
                .title(openApiProps.title())
                .description(openApiProps.description())
                .version(openApiProps.version())
                .contact(contact);
        return new OpenAPI()
                .info(info)
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .components(new Components().addSecuritySchemes("Bearer", createJwtTokenScheme()));
    }

    private SecurityScheme createJwtTokenScheme() {
        return new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("Bearer");
    }

    @ConfigurationProperties(prefix = "app.openapi")
    @Validated
    record OpenApiProperties(
            @DefaultValue("Blog API") String title,

            @DefaultValue("Blog API Swagger Documentation") String description,

            @DefaultValue("v1.0.0") String version,
            @Valid Contact contact) {

        public record Contact(
                @DefaultValue("example") String name,
                @DefaultValue("support@example.com") String email) {}
    }
}
