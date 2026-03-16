package br.com.infnet.controller;

import br.com.infnet.shared.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public String handleDomainException(DomainException ex,
                                         HttpServletRequest request,
                                         RedirectAttributes ra) {
        ra.addFlashAttribute("erro", ex.getMessage() != null ? ex.getMessage() : "Operação não permitida.");
        return redirectDestino(request);
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex,
                                   HttpServletRequest request,
                                   RedirectAttributes ra) {
        ra.addFlashAttribute("erro", "Erro inesperado: " + ex.getMessage());
        return redirectDestino(request);
    }

    private String redirectDestino(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/pedidos")) {
            return "redirect:/pedidos";
        }
        return "redirect:/produtos";
    }
}
