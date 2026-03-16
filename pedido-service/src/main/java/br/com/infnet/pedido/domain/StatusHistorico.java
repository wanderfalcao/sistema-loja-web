package br.com.infnet.pedido.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pedido_status_historico")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StatusHistorico {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusPedido statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusPedido statusNovo;

    @Column(length = 500)
    private String motivo;

    @CreatedDate
    @Column(name = "data_transicao", nullable = false, updatable = false)
    private LocalDateTime dataTransicao;

    @jakarta.persistence.PrePersist
    private void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

    public StatusHistorico(Pedido pedido, StatusPedido statusAnterior, StatusPedido statusNovo, String motivo) {
        this.pedido = pedido;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.motivo = motivo;
    }
}
