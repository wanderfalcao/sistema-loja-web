package br.com.infnet.pedido.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos", indexes = {
        @Index(name = "idx_pedido_status",       columnList = "status"),
        @Index(name = "idx_pedido_data_criacao", columnList = "data_criacao")
})
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Pedido {

    public static final int        MAX_DESCRICAO  = 255;
    public static final int        MAX_OBSERVACAO = 500;
    public static final BigDecimal VALOR_MINIMO   = new BigDecimal("0.01");

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = MAX_DESCRICAO, columnDefinition = "varchar(255)")
    private String descricao;

    @Embedded
    @AttributeOverride(name = "quantia", column = @Column(name = "valor", precision = 10, scale = 2, nullable = false))
    private Dinheiro valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Column(length = MAX_OBSERVACAO, columnDefinition = "varchar(500)")
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

    // ── Factory method ───────────────────────────────────────────────────────

    public static Pedido novo(String descricao, BigDecimal valor, String observacao) {
        Pedido p = new Pedido();
        p.id        = UUID.randomUUID();
        p.descricao = descricao;
        p.valor     = Dinheiro.de(valor);
        p.observacao = observacao;
        p.status    = StatusPedido.PENDENTE;
        return p;
    }

    // ── Behavior methods (command) ───────────────────────────────────────────

    /** Avança o status do pedido (chamado pela máquina de estados). */
    public void avancarStatus(StatusPedido novoStatus) {
        this.status = novoStatus;
        this.dataAtualizacao = java.time.LocalDateTime.now();
    }

    /** Atualiza os dados editáveis do pedido atomicamente. */
    public void atualizar(String descricao, Dinheiro valor, String observacao) {
        this.descricao       = descricao;
        this.valor           = valor;
        this.observacao      = observacao;
        this.dataAtualizacao = LocalDateTime.now();
    }

    /** Marca o pedido como contestado com o motivo registrado. */
    public void contestar(String motivo) {
        this.status          = StatusPedido.CONTESTADO;
        this.observacao      = motivo;
        this.dataAtualizacao = LocalDateTime.now();
    }

    // ── Query methods ────────────────────────────────────────────────────────

    public BigDecimal calcularTotal() {
        if (itens == null || itens.isEmpty()) return valor.quantia();
        return itens.stream()
                .map(i -> i.getSubtotal().quantia())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
