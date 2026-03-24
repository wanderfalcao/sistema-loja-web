package br.com.infnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/** Eureka Server — service discovery da stack. */
@SpringBootApplication(proxyBeanMethods = false)
@EnableEurekaServer
public final class EurekaServerApplication {

    private EurekaServerApplication() { }

    /**
     * Inicia o Eureka Server.
     *
     * @param args argumentos de linha de comando
     */
    public static void main(final String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
