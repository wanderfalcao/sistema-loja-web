package br.com.infnet.produto.domain;

import br.com.infnet.shared.exception.DomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "produtos")
@Getter
@Setter                          // gera setters para todos os campos…
@NoArgsConstructor               // exigido pelo JPA e pelo MapStruct
@EntityListeners(AuditingEntityListener.class)
public class Produto {

    public static final int MAX_NOME      = 255;
    public static final int MAX_DESCRICAO = 1000;
    public static final int MAX_SKU       = 50;
    public static final BigDecimal PRECO_MINIMO = BigDecimal.ZERO;

    private static final String ERR_PRECO_INVALIDO = "Preco deve ser maior que zero";

    @Id
    @Setter(AccessLevel.NONE)                        // … exceto os campos imutáveis
    private UUID id;

    @Column(nullable = false, length = MAX_NOME, unique = true)
    private String nome;

    @Column(length = MAX_DESCRICAO)
    private String descricao;

    @Column(nullable = false, unique = true, length = MAX_SKU)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(length = 500)
    private String imagemUrl;

    @Embedded
    @Setter(AccessLevel.NONE)                        // gerenciado via ativarPromocao/encerrarPromocao
    private Promocao promocao;

    @Column(nullable = false)
    private Integer estoque = 0;

    @Column(nullable = false)
    private Integer estoqueMinimo = 0;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CategoriaProduto categoria;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)                        // gerenciado pelo JPA Auditing
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column
    @Setter(AccessLevel.NONE)
    private LocalDateTime dataAtualizacao;

    // ── Fábrica ───────────────────────────────────────────────────────────────

    /** Único ponto de criação: aplica todas as invariantes de domínio. */
    public static Produto novo(String nome, String sku, BigDecimal preco) {
        if (nome == null || nome.trim().isEmpty()) throw new DomainException("Nome obrigatorio");
        if (sku == null || sku.trim().isEmpty())   throw new DomainException("SKU obrigatorio");
        if (preco == null || preco.compareTo(PRECO_MINIMO) <= 0) throw new DomainException(ERR_PRECO_INVALIDO);
        Produto p = new Produto();
        p.id           = UUID.randomUUID();
        p.nome         = nome.trim();
        p.sku          = sku.trim();
        p.preco        = preco;
        p.estoque      = 0;
        p.estoqueMinimo = 0;
        p.ativo        = true;
        return p;
    }

    // ── Comandos de domínio ───────────────────────────────────────────────────

    public void atualizar(String nome, String sku, BigDecimal preco) {
        if (nome == null || nome.trim().isEmpty()) throw new DomainException("Nome obrigatorio");
        if (sku == null || sku.trim().isEmpty())   throw new DomainException("SKU obrigatorio");
        if (preco == null || preco.compareTo(PRECO_MINIMO) <= 0) throw new DomainException(ERR_PRECO_INVALIDO);
        this.nome  = nome.trim();
        this.sku   = sku.trim();
        this.preco = preco;
    }

    public void ativar() {
        if (estoque == null || estoque <= estoqueMinimo)
            throw new DomainException("Produto sem estoque suficiente nao pode ser ativado");
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativarPromocao(BigDecimal percentual, LocalDateTime inicio, LocalDateTime fim) {
        if (!Boolean.TRUE.equals(this.ativo))
            throw new DomainException("Produto inativo nao pode ter promocao ativada");
        this.promocao = Promocao.criar(percentual, inicio, fim, this.preco);
    }

    public void encerrarPromocao() {
        this.promocao = null;
    }

    // ── Query de conveniência ─────────────────────────────────────────────────

    /** Preço com desconto quando há promoção ativa, ou {@code null}. */
    public BigDecimal getPrecoPromocional() {
        return promocao != null ? promocao.getPrecoComDesconto() : null;
    }
}
