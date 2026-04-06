package br.com.infnet.pedido.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "itens_pedido")
@Getter
@NoArgsConstructor
public class ItemPedido {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @PrePersist
    private void preCreate() {
        if (id == null) id = UUID.randomUUID();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "produto_id")
    private UUID produtoId;

    @Column(nullable = false, length = 255)
    private String nomeProduto;

    @Column(length = 50)
    private String skuProduto;

    @Embedded
    @AttributeOverride(name = "quantia", column = @Column(name = "preco_unitario", precision = 10, scale = 2, nullable = false))
    private Dinheiro precoUnitario;

    @Embedded
    @AttributeOverride(name = "inteiro", column = @Column(name = "quantidade", nullable = false))
    private Quantidade quantidade;

    @Embedded
    @AttributeOverride(name = "quantia", column = @Column(name = "subtotal", precision = 10, scale = 2, nullable = false))
    private Dinheiro subtotal;

    // ── Factory method ───────────────────────────────────────────────────────

    public static ItemPedido criar(Pedido pedido, UUID produtoId, String nomeProduto,
                                    String skuProduto, Dinheiro precoUnitario, Quantidade quantidade) {
        ItemPedido item = new ItemPedido();
        item.id            = UUID.randomUUID();
        item.pedido        = pedido;
        item.produtoId     = produtoId;
        item.nomeProduto   = nomeProduto != null ? nomeProduto.trim() : null;
        item.skuProduto    = skuProduto != null ? skuProduto.trim() : null;
        item.precoUnitario = precoUnitario;
        item.quantidade    = quantidade;
        item.subtotal      = precoUnitario.multiplicar(quantidade.inteiro());
        return item;
    }
}
