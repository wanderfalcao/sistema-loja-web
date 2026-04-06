package br.com.infnet.pedido.domain;

import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class DinheiroTest {

    @Test
    void de_valorZero_permitido() {
        Dinheiro d = Dinheiro.de(BigDecimal.ZERO);
        assertThat(d.quantia()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void de_valorPositivo_criaCorretamente() {
        Dinheiro d = Dinheiro.de(new BigDecimal("10.50"));
        assertThat(d.quantia()).isEqualByComparingTo("10.50");
    }

    @Test
    void de_valorNegativo_lancaDomainException() {
        assertThatThrownBy(() -> Dinheiro.de(new BigDecimal("-0.01")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void de_null_lancaDomainException() {
        assertThatThrownBy(() -> Dinheiro.de(null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void dePositivo_valorPositivo_criaCorretamente() {
        Dinheiro d = Dinheiro.dePositivo(new BigDecimal("5.00"));
        assertThat(d.quantia()).isEqualByComparingTo("5.00");
    }

    @Test
    void dePositivo_valorZero_lancaDomainException() {
        assertThatThrownBy(() -> Dinheiro.dePositivo(BigDecimal.ZERO))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void dePositivo_null_lancaDomainException() {
        assertThatThrownBy(() -> Dinheiro.dePositivo(null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void somar_doisValores_retornaSoma() {
        Dinheiro a = Dinheiro.de(new BigDecimal("10.00"));
        Dinheiro b = Dinheiro.de(new BigDecimal("5.50"));
        assertThat(a.somar(b).quantia()).isEqualByComparingTo("15.50");
    }

    @Test
    void multiplicar_fatorPositivo_retornaProduto() {
        Dinheiro d = Dinheiro.de(new BigDecimal("3.00"));
        assertThat(d.multiplicar(4).quantia()).isEqualByComparingTo("12.00");
    }

    @Test
    void equals_mesmosValores_saoIguais() {
        assertThat(Dinheiro.de(new BigDecimal("7.00")))
                .isEqualTo(Dinheiro.de(new BigDecimal("7.00")));
    }

    @Test
    void equals_valoresDiferentes_naoSaoIguais() {
        assertThat(Dinheiro.de(new BigDecimal("7.00")))
                .isNotEqualTo(Dinheiro.de(new BigDecimal("8.00")));
    }

    @Test
    void hashCode_mesmosValores_mesmoCodigo() {
        assertThat(Dinheiro.de(new BigDecimal("3.00")).hashCode())
                .isEqualTo(Dinheiro.de(new BigDecimal("3.00")).hashCode());
    }

    @Test
    void toString_retornaValorPlano() {
        assertThat(Dinheiro.de(new BigDecimal("2.50")).toString()).isEqualTo("2.50");
    }
}
