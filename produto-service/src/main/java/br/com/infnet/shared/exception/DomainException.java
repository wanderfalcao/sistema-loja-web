package br.com.infnet.shared.exception;

public class DomainException extends RuntimeException {
    public DomainException(String mensagem) {
        super(mensagem);
    }
}
