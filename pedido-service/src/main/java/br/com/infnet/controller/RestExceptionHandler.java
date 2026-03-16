package br.com.infnet.controller;

import br.com.infnet.shared.exception.DomainException;
import br.com.infnet.shared.exception.RecursoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @Value("${api.errors.base-type:https://loja.infnet.com.br/errors/}")
    private String baseType;

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(RecursoNaoEncontradoException ex,
                                                        HttpServletRequest request) {
        log.warn("Recurso não encontrado em {}: {}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), "not-found");
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex,
                                                               HttpServletRequest request) {
        log.warn("DomainException em {}: {}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage(), "domain-error");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Dados inválidos.");
        log.warn("Validação falhou em {}: {}", request.getRequestURI(), detail);
        return buildError(HttpStatus.BAD_REQUEST, "Dados inválidos", detail, "validation-error");
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ProblemDetail> handleHttpClientError(HttpClientErrorException ex,
                                                               HttpServletRequest request) {
        log.warn("Erro de comunicação com serviço externo em {}: {}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.BAD_GATEWAY, "Erro de gateway",
                "Serviço externo retornou erro: " + ex.getMessage(), "bad-gateway");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente.", "internal-error");
    }

    private ResponseEntity<ProblemDetail> buildError(HttpStatus status, String title,
                                                      String detail, String errorType) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(baseType + errorType));
        return ResponseEntity.status(status).body(pd);
    }
}
