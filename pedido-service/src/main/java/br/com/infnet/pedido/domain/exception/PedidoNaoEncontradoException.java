package br.com.infnet.pedido.domain.exception;

import br.com.infnet.shared.exception.RecursoNaoEncontradoException;

import java.util.UUID;

public class PedidoNaoEncontradoException extends RecursoNaoEncontradoException {

    public PedidoNaoEncontradoException(UUID id) {
        super("Pedido", id);
    }
}
