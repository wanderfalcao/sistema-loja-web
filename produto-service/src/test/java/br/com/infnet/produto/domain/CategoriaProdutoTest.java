package br.com.infnet.produto.domain;

import org.junit.jupiter.api.Test;

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
}
