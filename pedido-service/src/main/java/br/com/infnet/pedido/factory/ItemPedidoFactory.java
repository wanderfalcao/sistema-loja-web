package br.com.infnet.pedido.factory;

import br.com.infnet.pedido.domain.Dinheiro;
import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.Quantidade;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.shared.exception.DomainException;

import java.math.BigDecimal;

public class ItemPedidoFactory {

    private static final int MIN_QUANTIDADE = 1;

    private ItemPedidoFactory() {}

    public static ItemPedido criar(Pedido pedido, ItemPedidoRequest request) {
        if (request.getQuantidade() == null || request.getQuantidade() < MIN_QUANTIDADE)
            throw new DomainException("Quantidade mínima é 1.");
        if (request.getPrecoUnitario() == null ||
                request.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0)
            throw new DomainException("Preço unitário deve ser maior que zero.");

        return ItemPedido.criar(
            pedido,
            request.getProdutoId(),
            request.getNomeProduto(),
            request.getSkuProduto(),
            Dinheiro.dePositivo(request.getPrecoUnitario()),
            Quantidade.dePositivo(request.getQuantidade())
        );
    }
}
