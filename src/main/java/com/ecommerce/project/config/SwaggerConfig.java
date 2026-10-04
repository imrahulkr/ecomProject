package com.ecommerce.project.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
 @Bean
 public OpenAPI customOpenAPI() {
     SecurityScheme bearerScheme = new SecurityScheme()
             .type(SecurityScheme.Type.HTTP)
             .scheme("bearer")
             .bearerFormat("JWT")
             .description("JWT Bearer Token");

     SecurityRequirement bearerRequirement = new SecurityRequirement()
             .addList("Bearer Authorization");

     return new OpenAPI()
             .info(new Info()
                     .title("Spring Boot Ecommerce API Documentation")
                     .version("1.0.0")
                     .license(new License().name("Apache 2.0").url("myurl.com"))
                     .contact(new Contact().name("Rahul Kumar").email("rahul31774@gmil.com").url("myurl.in"))
             )
             .externalDocs(new ExternalDocumentation().description("Spring Boot Ecommerce API Documentation").url("myurl.com"))
             .components(new Components()
                     .addSecuritySchemes("Bearer Authorization", bearerScheme))
             .addSecurityItem(bearerRequirement);
 }
}