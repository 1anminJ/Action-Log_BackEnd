package com.project.actionlog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Action-Log API 명세서",
                description = "AI 회의록 요약 서비스 API",
                version = "v1.0"
        )
)
public class SwaggerConfig {
        @Bean
        public OpenAPI openAPI() {
                String jwtSchemeName = "BearerAuth";

                // 이 SecurityRequirement는 'models'에서 온 것입니다.
                SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

                // 이 SecurityScheme과 Type도 'models'에서 온 것입니다.
                Components components = new Components()
                        .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                                .name(jwtSchemeName)
                                .type(SecurityScheme.Type.HTTP) // HTTP 방식
                                .scheme("bearer")
                                .bearerFormat("JWT")); // Bearer 토큰

                return new OpenAPI()
                        .addSecurityItem(securityRequirement)
                        .components(components);
        }
}
