package br.com.hulysses.hulysses_one.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hulyssesOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Hulysses One API")
                .version("etapa-1")
                .description("Monólito modular de parceiros de negócio, catálogo de produtos e pedidos de venda."));
    }
}
