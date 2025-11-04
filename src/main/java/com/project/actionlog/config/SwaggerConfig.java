package com.project.actionlog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
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
}
