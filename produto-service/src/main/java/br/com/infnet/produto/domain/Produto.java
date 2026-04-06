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
@Table(name = "produtos", indexes = {
        @Index(name = "idx_produto_sku",  columnList = "sku"),
        @Index(name = "idx_produto_nome", columnList = "nome")
})
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Produto {

    public static final int MAX_NOME      = 255;
    public static final int MAX_DESCRICAO = 1000;
    public static final int MAX_SKU       = 50;

    private static final String ERR_PRECO_INVALIDO = "Preco deve ser maior que zero";

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = MAX_NOME, unique = true)
    private String nome;

    @Column(length = MAX_DESCRICAO)
    private String descricao;

    @Column(nullable = false, unique = true, length = MAX_SKU)
    @Convert(converter = SkuConverter.class)
    private Sku sku;

    @Embedded
    @AttributeOverride(name = "quantia", column = @Column(name = "preco", precision = 10, scale = 2, nullable = false))
    private Dinheiro preco;

    @Column(length = 500)
    private String imagemUrl;

    @Embedded
    @Setter(AccessLevel.NONE)
    private Promocao promocao;

    @Embedded
    @AttributeOverride(name = "inteiro", column = @Column(name = "estoque", nullable = false))
    private Quantidade estoque;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "inteiro", column = @Column(name = "estoque_minimo", nullable = false))
    })
    private Quantidade estoqueMinimo;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CategoriaProduto categoria;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column
    private LocalDateTime dataAtualizacao;

    // ── Factory methods ──────────────────────────────────────────────────────

    public static Produto novo(String nome, Sku sku, BigDecimal preco) {
        if (nome == null || nome.trim().isEmpty()) throw new DomainException("Nome obrigatorio");
        if (sku == null)                           throw new DomainException("SKU obrigatorio");
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) throw new DomainException(ERR_PRECO_INVALIDO);
        Produto p = new Produto();
        p.id            = UUID.randomUUID();
        p.nome          = nome.trim();
        p.sku           = sku;
        p.preco         = Dinheiro.dePositivo(preco);
        p.estoque       = Quantidade.de(0);
        p.estoqueMinimo = Quantidade.de(0);
        p.ativo         = true;
        return p;
    }

    // ── Behavior methods (command) ───────────────────────────────────────────

    public void atualizar(String nome, Sku sku, BigDecimal preco) {
        if (nome == null || nome.trim().isEmpty()) throw new DomainException("Nome obrigatorio");
        if (sku == null)                           throw new DomainException("SKU obrigatorio");
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) throw new DomainException(ERR_PRECO_INVALIDO);
        this.nome  = nome.trim();
        this.sku   = sku;
        this.preco = Dinheiro.dePositivo(preco);
    }

    public void ativar() {
        if (!estoque.maiorQue(estoqueMinimo))
            throw new DomainException("Produto sem estoque suficiente nao pode ser ativado");
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    /** Ajusta o estoque usando polimorfismo total de {@link TipoOperacaoEstoque}. */
    public void ajustarEstoque(TipoOperacaoEstoque operacao, Quantidade quantidade) {
        operacao.validarAntesDeAplicar(Boolean.TRUE.equals(this.ativo));
        this.estoque = operacao.aplicar(this.estoque, quantidade);
        sincronizarStatusComEstoque();
    }

    private void sincronizarStatusComEstoque() {
        if (!this.estoque.maiorQue(this.estoqueMinimo) && Boolean.TRUE.equals(this.ativo))
            this.ativo = false;
        else if (this.estoque.maiorQue(this.estoqueMinimo) && Boolean.FALSE.equals(this.ativo))
            this.ativo = true;
    }

    public void ativarPromocao(BigDecimal percentual, LocalDateTime inicio, LocalDateTime fim) {
        if (!Boolean.TRUE.equals(this.ativo))
            throw new DomainException("Produto inativo nao pode ter promocao ativada");
        if (this.categoria != null && percentual.compareTo(this.categoria.descontoMaximoPermitido()) > 0)
            throw new DomainException("Desconto máximo para " + this.categoria.getLabel()
                + " é " + this.categoria.descontoMaximoPermitido() + "%");
        this.promocao = Promocao.criar(percentual, inicio, fim, this.preco.quantia());
    }

    public void encerrarPromocao() {
        this.promocao = null;
    }

    // ── Definition methods (initialization-time setters with semantic names) ─

    public void definirDescricao(String descricao) {
        this.descricao = descricao;
    }

    public void definirEstoque(Quantidade estoque) {
        this.estoque = estoque;
    }

    public void definirEstoqueMinimo(Quantidade estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public void definirImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public void definirCategoria(CategoriaProduto categoria) {
        this.categoria = categoria;
    }

    public void alterarAtivo(Boolean ativo) {
        if (ativo != null) this.ativo = ativo;
    }

    // ── Query methods ────────────────────────────────────────────────────────

    public BigDecimal getPrecoPromocional() {
        return promocao != null ? promocao.getPrecoComDesconto() : null;
    }
}
