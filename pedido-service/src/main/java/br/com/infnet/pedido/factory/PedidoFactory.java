package br.com.infnet.pedido.factory;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.dto.PedidoRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PedidoFactory {

    public static Pedido criar(String descricao, BigDecimal valor, String observacao) {
        Pedido p = new Pedido();
        p.setId(UUID.randomUUID());
        p.setDescricao(descricao.trim());
        p.setValor(valor);
        p.setStatus(StatusPedido.PENDENTE);
        p.setObservacao(observacao != null ? observacao.trim() : null);
        p.setDataCriacao(LocalDateTime.now());
        return p;
    }

    public static Pedido criar(PedidoRequest request) {
        return criar(request.getDescricao(), request.getValor(), request.getObservacao());
    }
}
