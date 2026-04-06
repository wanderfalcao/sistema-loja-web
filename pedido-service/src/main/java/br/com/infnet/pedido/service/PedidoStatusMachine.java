package br.com.infnet.pedido.service;

import br.com.infnet.client.TipoOperacaoEstoque;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.shared.exception.DomainException;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Encapsula as regras de transição de status de um pedido e determina
 * se a mudança de status implica alguma operação de estoque.
 */
@Component
class PedidoStatusMachine {

    private static final String MSG_TRANSICAO_INVALIDA = "Transição inválida: ";

    /**
     * Valida se a transição de {@code atual} para {@code novo} é permitida.
     * Lança {@link DomainException} quando a transição não é reconhecida pelo fluxo.
     *
     * @param atual status atual do pedido
     * @param novo  status pretendido
     */
    void validarTransicao(StatusPedido atual, StatusPedido novo) {
        boolean valida = switch (atual) {
            case PENDENTE    -> novo == StatusPedido.PROCESSANDO || novo == StatusPedido.CANCELADO;
            case PROCESSANDO -> novo == StatusPedido.CONCLUIDO   || novo == StatusPedido.CANCELADO;
            case CONCLUIDO   -> novo == StatusPedido.CONTESTADO;
            case CONTESTADO  -> novo == StatusPedido.PROCESSANDO || novo == StatusPedido.CANCELADO;
            case CANCELADO   -> false;
        };
        if (!valida)
            throw new DomainException(MSG_TRANSICAO_INVALIDA + atual + " → " + novo);
    }

    /**
     * Determina a operação de estoque associada à transição, se houver.
     *
     * <ul>
     *   <li>PENDENTE → PROCESSANDO: {@link TipoOperacaoEstoque#SAIDA} (reserva)</li>
     *   <li>PROCESSANDO → CANCELADO: {@link TipoOperacaoEstoque#ENTRADA} (estorno)</li>
     *   <li>Demais transições: sem impacto no estoque</li>
     * </ul>
     *
     * @param atual status atual do pedido
     * @param novo  status pretendido
     * @return operação de estoque, ou {@link Optional#empty()} se não houver impacto
     */
    Optional<TipoOperacaoEstoque> resolverOperacaoEstoque(StatusPedido atual, StatusPedido novo) {
        if (atual == StatusPedido.PENDENTE && novo == StatusPedido.PROCESSANDO)
            return Optional.of(TipoOperacaoEstoque.SAIDA);
        if (atual == StatusPedido.PROCESSANDO && novo == StatusPedido.CANCELADO)
            return Optional.of(TipoOperacaoEstoque.ENTRADA);
        return Optional.empty();
    }
}
