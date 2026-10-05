package br.com.hulysses.business_partner_service.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hulyssesOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Business Partner Service API")
                .version("etapa-2")
                .description("Cadastro independente de pessoas e empresas, contatos, endereços e elegibilidade por papel."));
    }
}
