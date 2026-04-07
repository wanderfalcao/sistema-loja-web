package br.com.infnet.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expõe propriedades de configuração para todos os templates Thymeleaf.
 * Permite que o sidebar use a URL correta do produto-service independente do ambiente
 * (localhost em dev, URL do Cloud Run em prod).
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${app.produto.base-url:http://localhost:8081}")
    private String produtoBaseUrl;

    @ModelAttribute("produtoBaseUrl")
    public String produtoBaseUrl() {
        return produtoBaseUrl;
    }
}
