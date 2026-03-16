package br.com.infnet.pedido.domain;

import br.com.infnet.shared.exception.DomainException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Pedido {

    public static final int        MAX_DESCRICAO  = 255;
    public static final int        MAX_OBSERVACAO = 500;
    public static final BigDecimal VALOR_MINIMO   = new BigDecimal("0.01");

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = MAX_DESCRICAO)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Column(length = MAX_OBSERVACAO)
    private String observacao;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    @jakarta.persistence.PrePersist
    private void preCreate() {
        if (id == null) id = java.util.UUID.randomUUID();
    }

    // ── Fábrica ───────────────────────────────────────────────────────────────

    public static Pedido novo(String descricao, BigDecimal valor, String observacao) {
        Pedido p = new Pedido();
        p.id = UUID.randomUUID();
        p.descricao = descricao;
        p.valor = valor;
        p.observacao = observacao;
        p.status = StatusPedido.PENDENTE;
        return p;
    }

    // ── Comandos de domínio ───────────────────────────────────────────────────

    public void avancarStatus(StatusPedido novoStatus) {
        this.status = novoStatus;
        this.dataAtualizacao = java.time.LocalDateTime.now();
    }

    /**
     * Retorna a soma dos subtotais dos itens; ou o valor manual se a lista estiver vazia.
     * Garante imutabilidade do total mesmo que o produto original mude depois.
     */
    public BigDecimal calcularTotal() {
        if (itens == null || itens.isEmpty()) return valor;
        return itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
