package br.com.infnet.pedido.domain;

import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class QuantidadeTest {

    @Test
    void de_valorZero_permitido() {
        Quantidade q = Quantidade.de(0);
        assertThat(q.inteiro()).isZero();
    }

    @Test
    void de_valorPositivo_criaCorretamente() {
        Quantidade q = Quantidade.de(5);
        assertThat(q.inteiro()).isEqualTo(5);
    }

    @Test
    void de_valorNegativo_lancaDomainException() {
        assertThatThrownBy(() -> Quantidade.de(-1))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("negativa");
    }

    @Test
    void dePositivo_valorPositivo_criaCorretamente() {
        Quantidade q = Quantidade.dePositivo(3);
        assertThat(q.inteiro()).isEqualTo(3);
    }

    @Test
    void dePositivo_valorZero_lancaDomainException() {
        assertThatThrownBy(() -> Quantidade.dePositivo(0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void dePositivo_valorNegativo_lancaDomainException() {
        assertThatThrownBy(() -> Quantidade.dePositivo(-5))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void somar_doisValores_retornaSoma() {
        Quantidade a = Quantidade.de(3);
        Quantidade b = Quantidade.de(4);
        assertThat(a.somar(b).inteiro()).isEqualTo(7);
    }

    @Test
    void subtrair_valorMenor_retornaDiferenca() {
        Quantidade a = Quantidade.de(10);
        Quantidade b = Quantidade.de(3);
        assertThat(a.subtrair(b).inteiro()).isEqualTo(7);
    }

    @Test
    void subtrair_valorMaior_lancaDomainException() {
        Quantidade a = Quantidade.de(2);
        Quantidade b = Quantidade.de(5);
        assertThatThrownBy(() -> a.subtrair(b))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("insuficiente");
    }

    @Test
    void maiorQue_retornaTrue_quandoMaior() {
        assertThat(Quantidade.de(5).maiorQue(Quantidade.de(3))).isTrue();
    }

    @Test
    void maiorQue_retornaFalse_quandoIgual() {
        assertThat(Quantidade.de(5).maiorQue(Quantidade.de(5))).isFalse();
    }

    @Test
    void maiorQue_retornaFalse_quandoMenor() {
        assertThat(Quantidade.de(2).maiorQue(Quantidade.de(5))).isFalse();
    }

    @Test
    void equals_mesmosValores_saoIguais() {
        assertThat(Quantidade.de(4)).isEqualTo(Quantidade.de(4));
    }

    @Test
    void equals_valoresDiferentes_naoSaoIguais() {
        assertThat(Quantidade.de(4)).isNotEqualTo(Quantidade.de(5));
    }

    @Test
    void hashCode_mesmosValores_mesmoCodigo() {
        assertThat(Quantidade.de(7).hashCode()).isEqualTo(Quantidade.de(7).hashCode());
    }

    @Test
    void toString_retornaValorComoString() {
        assertThat(Quantidade.de(9).toString()).isEqualTo("9");
    }
}
