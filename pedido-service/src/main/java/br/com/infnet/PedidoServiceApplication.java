package br.com.infnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/** Pedido Service — microsserviço de gestão de pedidos. */
@EnableRetry
@SpringBootApplication(proxyBeanMethods = false)
public final class PedidoServiceApplication {

    private PedidoServiceApplication() { }

    /**
     * Inicia o Pedido Service.
     *
     * @param args argumentos de linha de comando
     */
    public static void main(final String[] args) {
        SpringApplication.run(PedidoServiceApplication.class, args);
    }
}
