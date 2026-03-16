package br.com.infnet.pedido.factory;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;

import java.math.BigDecimal;

public class PedidoTestFactory {

    public static Pedido pedidoPendente() {
        return PedidoFactory.criar("Test", new BigDecimal("10.00"), null);
    }

    public static Pedido pedidoCom(StatusPedido status) {
        Pedido p = pedidoPendente();
        p.setStatus(status);
        return p;
    }
}
