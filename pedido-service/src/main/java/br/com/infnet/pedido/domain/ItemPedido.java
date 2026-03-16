package br.com.infnet.pedido.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "itens_pedido")
@Getter
@Setter
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

    // Contrato de integração futura:
    // popular via GET /api/v1/produtos/{produtoId} no serviço tp2-web-crud (porta 8080)
    @Column(name = "produto_id")
    private UUID produtoId;

    // Snapshot imutável do produto no momento do pedido
    @Column(nullable = false, length = 255)
    private String nomeProduto;

    @Column(length = 50)
    private String skuProduto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
