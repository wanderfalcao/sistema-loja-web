package br.com.infnet.pedido.service;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.shared.exception.DomainException;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Valida as entradas de negócio do {@link PedidoService} antes de qualquer persistência.
 * Lança {@link DomainException} para cada violação de regra.
 */
@Component
class PedidoValidador {

    private static final String MSG_DESCRICAO_VAZIA    = "Descrição não pode ser vazia.";
    private static final String MSG_DESCRICAO_LONGA    = "Descrição deve ter no máximo " + Pedido.MAX_DESCRICAO + " caracteres.";
    private static final String MSG_VALOR_MINIMO       = "Valor deve ser no mínimo R$ 0,01.";
    private static final String MSG_MOTIVO_OBRIGATORIO = "Motivo da contestação não pode ser vazio.";

    /**
     * Verifica se a descrição não é nula, vazia ou supera {@link Pedido#MAX_DESCRICAO} caracteres.
     *
     * @param descricao texto a validar
     */
    void validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank())
            throw new DomainException(MSG_DESCRICAO_VAZIA);
        if (descricao.trim().length() > Pedido.MAX_DESCRICAO)
            throw new DomainException(MSG_DESCRICAO_LONGA);
    }

    /**
     * Verifica se o valor é pelo menos {@link Pedido#VALOR_MINIMO}.
     *
     * @param valor quantia a validar
     */
    void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(Pedido.VALOR_MINIMO) < 0)
            throw new DomainException(MSG_VALOR_MINIMO);
    }

    /**
     * Verifica se o motivo de contestação não é nulo nem vazio.
     *
     * @param motivo texto a validar
     */
    void validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank())
            throw new DomainException(MSG_MOTIVO_OBRIGATORIO);
    }
}
