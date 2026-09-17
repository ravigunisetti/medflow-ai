package com.phcnet.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI phcNetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PHC-NET AI Core API")
                        .description("REST API Gateway & Core Domain Services for India Primary Health Centre (PHC) Medicine Supply Chain Platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PHC-NET AI Architecture Team")
                                .email("contact@phcnet.gov.in"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}
