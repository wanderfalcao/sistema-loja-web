package br.com.infnet.produto.fuzz;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.SkuGenerator;
import br.com.infnet.shared.exception.DomainException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Positive;
import net.jqwik.api.constraints.Scale;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de propriedade (jqwik) do produto-service.
 *
 * P1 — Todo produto válido criado pode ser recuperado com os dados intactos.
 * P2 — Qualquer preço > 0 com nome e categoria válidos não lança exceção.
 * P3 — SKUs gerados são únicos a cada chamada ao SkuGenerator.
 */
class ProdutoFuzzTest {

    /**
     * P1 — Produto criado com dados válidos preserva nome, sku e preço.
     */
    @Property(tries = 200)
    void p1_produtoValido_preservaDados(
            @ForAll @NotBlank String nome,
            @ForAll @Positive @Scale(2) BigDecimal preco) {

        String nomeNormalizado = nome.trim().replaceAll("\\s+", " ");
        Assume.that(!nomeNormalizado.isBlank());
        Assume.that(nomeNormalizado.length() <= Produto.MAX_NOME);

        String sku = SkuGenerator.fromNome(nomeNormalizado);
        Produto p = Produto.novo(nomeNormalizado, sku, preco);

        assertThat(p.getNome()).isEqualTo(nomeNormalizado);
        assertThat(p.getSku()).isEqualTo(sku);
        assertThat(p.getPreco()).isEqualByComparingTo(preco);
        assertThat(p.getId()).isNotNull();
    }

    /**
     * P2 — Preço positivo com nome e categoria válidos nunca lança exceção.
     */
    @Property(tries = 300)
    void p2_precoPositivo_semprePermiteCriacao(
            @ForAll @NotBlank String nome,
            @ForAll @Positive @Scale(2) BigDecimal preco,
            @ForAll CategoriaProduto categoria) {

        String nomeNormalizado = nome.trim();
        Assume.that(!nomeNormalizado.isBlank());
        Assume.that(nomeNormalizado.length() <= Produto.MAX_NOME);

        String sku = SkuGenerator.fromNome(nomeNormalizado);
        Produto p = Produto.novo(nomeNormalizado, sku, preco);
        p.setCategoria(categoria);

        assertThat(p.getPreco()).isGreaterThan(BigDecimal.ZERO);
        assertThat(p.getCategoria()).isEqualTo(categoria);
    }

    /**
     * P3 — fromNome sempre retorna SKU não-nulo, não-vazio e com sufixo hexadecimal de 4 chars.
     */
    @Property(tries = 100)
    void p3_skuGerado_temFormatoValido(@ForAll @NotBlank String nome) {
        Assume.that(!nome.trim().isBlank());

        String sku = SkuGenerator.fromNome(nome.trim());

        assertThat(sku).isNotNull().isNotBlank();
        // sufixo sempre são 4 chars hexadecimais maiúsculos
        assertThat(sku).matches(".*-[0-9A-F]{4}$");
    }

    /**
     * P3b — Preço negativo ou zero sempre rejeita a criação do produto.
     */
    @Property(tries = 200)
    void p3b_precoNegativoOuZero_sempreLancaExcecao(
            @ForAll @Scale(2) BigDecimal preco) {

        Assume.that(preco.compareTo(BigDecimal.ZERO) <= 0);

        assertThatThrownBy(() -> Produto.novo("Produto Valido", "SKU-001", preco))
                .isInstanceOf(DomainException.class);
    }
}
