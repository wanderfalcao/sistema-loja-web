package br.com.infnet.pedido.factory;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.dto.PedidoRequest;

import java.math.BigDecimal;

public class PedidoFactory {

    private PedidoFactory() {}

    public static Pedido criar(String descricao, BigDecimal valor, String observacao) {
        return Pedido.novo(descricao != null ? descricao.trim() : null, valor, observacao);
    }

    public static Pedido criar(PedidoRequest request) {
        return Pedido.novo(request.getDescricao(), request.getValor(), request.getObservacao());
    }
}
