package br.com.infnet.controller;

import br.com.infnet.shared.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public String handleDomainException(DomainException ex,
                                         HttpServletRequest request,
                                         RedirectAttributes ra) {
        ra.addFlashAttribute("erro", ex.getMessage() != null ? ex.getMessage() : "Operação não permitida.");
        return "redirect:/pedidos";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex,
                                   HttpServletRequest request,
                                   RedirectAttributes ra) {
        ra.addFlashAttribute("erro", "Erro inesperado: " + ex.getMessage());
        return "redirect:/pedidos";
    }
}
