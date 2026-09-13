package com.trip.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stay Search API")
                        .description("여러 숙박 공급사의 상품을 하나의 모델로 통합해 검색하는 API")
                        .version("v1"))
                .servers(List.of(new Server().url("/").description("현재 서버")));
    }
}
