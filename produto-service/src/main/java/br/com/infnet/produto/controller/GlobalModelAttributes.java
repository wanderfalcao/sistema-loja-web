package br.com.infnet.produto.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expõe propriedades de configuração para todos os templates Thymeleaf.
 * Permite que o sidebar use a URL correta do pedido-service independente do ambiente
 * (localhost em dev, URL do Cloud Run em prod).
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${app.pedido.base-url:http://localhost:8082}")
    private String pedidoBaseUrl;

    @ModelAttribute("pedidoBaseUrl")
    public String pedidoBaseUrl() {
        return pedidoBaseUrl;
    }
}
