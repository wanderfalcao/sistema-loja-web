package br.com.infnet.produto.domain.exception;

import br.com.infnet.shared.exception.RecursoNaoEncontradoException;

import java.util.UUID;

public class ProdutoNaoEncontradoException extends RecursoNaoEncontradoException {
    public ProdutoNaoEncontradoException(UUID id) {
        super("Produto", id);
    }
}
