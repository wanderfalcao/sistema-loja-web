package br.com.infnet.pedido.factory;

import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.shared.exception.DomainException;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemPedidoFactory {

    private static final int MIN_QUANTIDADE = 1;

    public static ItemPedido criar(Pedido pedido, ItemPedidoRequest request) {
        if (request.getQuantidade() == null || request.getQuantidade() < MIN_QUANTIDADE) {
            throw new DomainException("Quantidade mínima é 1.");
        }
        if (request.getPrecoUnitario() == null ||
                request.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Preço unitário deve ser maior que zero.");
        }

        ItemPedido item = new ItemPedido();
        item.setId(UUID.randomUUID());
        item.setPedido(pedido);
        item.setProdutoId(request.getProdutoId());
        item.setNomeProduto(request.getNomeProduto().trim());
        item.setSkuProduto(request.getSkuProduto() != null ? request.getSkuProduto().trim() : null);
        item.setPrecoUnitario(request.getPrecoUnitario());
        item.setQuantidade(request.getQuantidade());
        item.setSubtotal(request.getPrecoUnitario()
                .multiply(BigDecimal.valueOf(request.getQuantidade())));
        return item;
    }
}
