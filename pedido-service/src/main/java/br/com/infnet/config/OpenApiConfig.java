package br.com.infnet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pedido Service API")
                        .description("Gerenciamento de pedidos: criação, fluxo de status e histórico de auditoria.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Infnet — Engenharia de Software")
                                .email("aluno@infnet.edu.br")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway"),
                        new Server().url("http://localhost:8082").description("Acesso direto (dev)")
                ));
    }
}
