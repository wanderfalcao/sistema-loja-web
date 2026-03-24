package br.com.infnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** API Gateway — ponto de entrada único da aplicação. */
@SpringBootApplication(proxyBeanMethods = false)
public final class ApiGatewayApplication {

    private ApiGatewayApplication() { }

    /**
     * Inicia o API Gateway.
     *
     * @param args argumentos de linha de comando
     */
    public static void main(final String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
