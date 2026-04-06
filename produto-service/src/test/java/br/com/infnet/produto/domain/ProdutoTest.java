package br.com.infnet.produto.domain;

import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ProdutoTest {

    // ── novo(nome, sku, preco) ────────────────────────────────────────────────

    @Test
    void novoDeveGerarUUID() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("100"));
        assertThat(p.getId()).isNotNull();
        assertThat(p.getSku().codigo()).isEqualTo("MON-001");
        assertThat(p.getEstoque().inteiro()).isZero();
        assertThat(p.getAtivo()).isTrue();
    }

    @Test
    void novoDeveRejeitarSkuNulo() {
        assertThatThrownBy(() -> Produto.novo("Monitor", null, new BigDecimal("100")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("SKU obrigatorio");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void skuDeDeveRejeitarValorInvalido(String sku) {
        assertThatThrownBy(() -> Sku.de(sku))
                .isInstanceOf(DomainException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void novoDeveRejeitarNomeInvalido(String nome) {
        assertThatThrownBy(() -> Produto.novo(nome, Sku.de("SKU-001"), new BigDecimal("100")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Nome obrigatorio");
    }

    @Test
    void novoDeveRejeitarPrecoNegativo() {
        assertThatThrownBy(() -> Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("-1")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Preco deve ser maior que zero");
    }

    @Test
    void novoDeveRejeitarPrecoNulo() {
        assertThatThrownBy(() -> Produto.novo("Monitor", Sku.de("MON-001"), null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Preco deve ser maior que zero");
    }

    // ── atualizar(nome, sku, preco) ───────────────────────────────────────────

    @Test
    void atualizarDeveAlterarCampos() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("100"));
        p.atualizar("Monitor Pro", Sku.de("MON-PRO-001"), new BigDecimal("200"));

        assertThat(p.getNome()).isEqualTo("Monitor Pro");
        assertThat(p.getSku().codigo()).isEqualTo("MON-PRO-001");
        assertThat(p.getPreco().quantia()).isEqualByComparingTo("200");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void atualizarDeveRejeitarNomeInvalido(String nome) {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("100"));
        assertThatThrownBy(() -> p.atualizar(nome, Sku.de("MON-001"), new BigDecimal("100")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Nome obrigatorio");
    }

    @Test
    void atualizarDeveRejeitarSkuNulo() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("100"));
        assertThatThrownBy(() -> p.atualizar("Monitor Pro", null, new BigDecimal("100")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("SKU obrigatorio");
    }

    @Test
    void atualizarDeveRejeitarPrecoNegativo() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("100"));
        assertThatThrownBy(() -> p.atualizar("Monitor", Sku.de("MON-001"), new BigDecimal("-1")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Preco deve ser maior que zero");
    }

    // ── Métodos semânticos ────────────────────────────────────────────────────

    @Test
    void metodosSemanticosDevemAtualizarEstadoControlado() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.definirDescricao("Monitor 4K");
        p.definirEstoque(Quantidade.de(5));
        p.definirEstoqueMinimo(Quantidade.de(2));
        p.definirImagemUrl("https://img.loja.com/mon.jpg");
        p.definirCategoria(CategoriaProduto.MONITORES);

        assertThat(p.getDescricao()).isEqualTo("Monitor 4K");
        assertThat(p.getEstoque().inteiro()).isEqualTo(5);
        assertThat(p.getEstoqueMinimo().inteiro()).isEqualTo(2);
        assertThat(p.getImagemUrl()).isEqualTo("https://img.loja.com/mon.jpg");
        assertThat(p.getCategoria()).isEqualTo(CategoriaProduto.MONITORES);
    }

    // ── ativarPromocao / encerrarPromocao ─────────────────────────────────────

    @Test
    void deveAtivarPromocaoComPercentual() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));

        p.ativarPromocao(new BigDecimal("20"), null, null);

        assertThat(p.getPromocao()).isNotNull();
        assertThat(p.getPrecoPromocional()).isEqualByComparingTo("800.00");
        assertThat(p.getPromocao().getPercentualDesconto()).isEqualByComparingTo("20");
    }

    @Test
    void deveAtivarPromocaoComDatas() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fim = LocalDateTime.now().plusDays(7);

        p.ativarPromocao(new BigDecimal("10"), inicio, fim);

        assertThat(p.getPromocao().getInicio()).isEqualTo(inicio);
        assertThat(p.getPromocao().getFim()).isEqualTo(fim);
        assertThat(p.getPrecoPromocional()).isEqualByComparingTo("900.00");
    }

    @Test
    void deveEncerrarPromocao() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.ativarPromocao(new BigDecimal("20"), null, null);
        p.encerrarPromocao();

        assertThat(p.getPromocao()).isNull();
        assertThat(p.getPrecoPromocional()).isNull();
    }

    @Test
    void deveRejeitarPromocaoEmProdutoInativo() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.desativar();

        assertThatThrownBy(() -> p.ativarPromocao(new BigDecimal("20"), null, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void deveRejeitarPercentualZero() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));

        assertThatThrownBy(() -> p.ativarPromocao(BigDecimal.ZERO, null, null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deveRejeitarPercentual100() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));

        assertThatThrownBy(() -> p.ativarPromocao(new BigDecimal("100"), null, null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deveRejeitarDataFimAnteriorAoInicio() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        LocalDateTime inicio = LocalDateTime.now().plusDays(5);
        LocalDateTime fim = LocalDateTime.now().plusDays(1);

        assertThatThrownBy(() -> p.ativarPromocao(new BigDecimal("20"), inicio, fim))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("fim");
    }

    @Test
    void getPrecoPromocionalRetornaNullSemPromocao() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        assertThat(p.getPrecoPromocional()).isNull();
    }

    // ── ativar() ──────────────────────────────────────────────────────────────

    @Test
    void deveAtivarProdutoComEstoqueSuficiente() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.desativar();
        p.definirEstoque(Quantidade.de(10));
        p.definirEstoqueMinimo(Quantidade.de(5));

        p.ativar();

        assertThat(p.getAtivo()).isTrue();
    }

    @Test
    void deveRejeitarAtivacaoComEstoqueAbaixoDoMinimo() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.desativar();
        p.definirEstoque(Quantidade.de(3));
        p.definirEstoqueMinimo(Quantidade.de(5));

        assertThatThrownBy(() -> p.ativar())
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("estoque");
    }

    @Test
    void deveRejeitarAtivacaoComEstoqueIgualAoMinimo() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.desativar();
        p.definirEstoque(Quantidade.de(5));
        p.definirEstoqueMinimo(Quantidade.de(5));

        assertThatThrownBy(() -> p.ativar())
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("estoque");
    }

    @Test
    void deveRejeitarPromocaoComPercentualNulo() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));

        assertThatThrownBy(() -> p.ativarPromocao(null, null, null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deveRejeitarPromocaoComFimIgualAoInicio() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        LocalDateTime dt = LocalDateTime.now().plusDays(1);

        assertThatThrownBy(() -> p.ativarPromocao(new BigDecimal("20"), dt, dt))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("fim");
    }

    @Test
    void deveDefinirCategoria() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.definirCategoria(CategoriaProduto.MONITORES);

        assertThat(p.getCategoria()).isEqualTo(CategoriaProduto.MONITORES);
    }

    // ── Desconto máximo por categoria ─────────────────────────────────────────

    @Test
    void deveRejeitarPromocaoAcimaDoMaximoDaCategoria() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.definirCategoria(CategoriaProduto.MONITORES);

        assertThatThrownBy(() -> p.ativarPromocao(new BigDecimal("35"), null, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Desconto máximo");
    }

    @Test
    void devePermitirPromocaoAbaixoDoMaximoDaCategoria() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        p.definirCategoria(CategoriaProduto.MONITORES);

        assertThatNoException()
                .isThrownBy(() -> p.ativarPromocao(new BigDecimal("25"), null, null));
    }

    @Test
    void devePermitirPromocaoSemCategoriaDefinida() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));

        assertThatNoException()
                .isThrownBy(() -> p.ativarPromocao(new BigDecimal("80"), null, null));
    }

    // ── Duração mínima de promoção ────────────────────────────────────────────

    @Test
    void deveRejeitarPromocaoComDuracaoInferiorA1Hora() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fim = inicio.plusMinutes(30);

        assertThatThrownBy(() -> p.ativarPromocao(new BigDecimal("10"), inicio, fim))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("1 hora");
    }

    @Test
    void devePermitirPromocaoComDuracaoExataDe1Hora() {
        Produto p = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fim = inicio.plusHours(1);

        assertThatNoException()
                .isThrownBy(() -> p.ativarPromocao(new BigDecimal("10"), inicio, fim));
    }
}
