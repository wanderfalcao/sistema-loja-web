package br.com.infnet.produto.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class CategoriaProdutoTest {

    @Test
    void todosOsValoresTemLabelNaoVazio() {
        for (CategoriaProduto c : CategoriaProduto.values()) {
            assertThat(c.getLabel()).isNotBlank();
        }
    }

    @Test
    void labelDeMonitoresEstaCorreto() {
        assertThat(CategoriaProduto.MONITORES.getLabel()).isEqualTo("Monitores");
    }

    @Test
    void totalDeCategoriasEsperado() {
        assertThat(CategoriaProduto.values()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void descontoMaximoPorCategoriaDeveSerDefinido() {
        assertThat(CategoriaProduto.MONITORES.descontoMaximoPermitido()).isEqualByComparingTo("30");
        assertThat(CategoriaProduto.COMPONENTES.descontoMaximoPermitido()).isEqualByComparingTo("20");
        assertThat(CategoriaProduto.GERAL.descontoMaximoPermitido()).isEqualByComparingTo("50");
    }

    @Test
    void todasCategoriasDevemTerDescontoMaximoPositivo() {
        for (CategoriaProduto c : CategoriaProduto.values()) {
            assertThat(c.descontoMaximoPermitido())
                .as("Categoria %s deve ter desconto máximo > 0", c)
                .isGreaterThan(BigDecimal.ZERO);
        }
    }
}
