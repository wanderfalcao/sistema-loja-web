package br.com.infnet.shared.exception;

import java.util.UUID;

public class RecursoNaoEncontradoException extends DomainException {
    public RecursoNaoEncontradoException(String tipo, UUID id) {
        super(tipo + " não encontrado(a) para ID: " + id);
    }
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
