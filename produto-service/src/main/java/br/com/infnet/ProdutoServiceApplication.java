package br.com.infnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Produto Service — microsserviço de catálogo de produtos. */
@SpringBootApplication(proxyBeanMethods = false)
public final class ProdutoServiceApplication {

    private ProdutoServiceApplication() { }

    /**
     * Inicia o Produto Service.
     *
     * @param args argumentos de linha de comando
     */
    public static void main(final String[] args) {
        SpringApplication.run(ProdutoServiceApplication.class, args);
    }
}
