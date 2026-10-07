package br.com.gerdau.servicos.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Padronização de Códigos de Serviço — Gerdau")
                        .version("v1")
                        .description("Busca, similaridade, cadastro de itens, códigos completos e governança."))
                .components(new Components()
                        .addSecuritySchemes("basic", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("basic"))
                        .addSecuritySchemes("bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("basic"))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
