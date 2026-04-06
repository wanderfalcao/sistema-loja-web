package br.com.infnet.pedido.service;

import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.client.TipoOperacaoEstoque;
import br.com.infnet.pedido.domain.Pedido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Coordena os ajustes de estoque junto ao {@code produto-service} para cada item de um pedido.
 * Ignora itens sem {@code produtoId}, pois esses não possuem referência rastreável de estoque.
 */
@Component
@RequiredArgsConstructor
class EstoqueOrquestrador {

    private final ProdutoServiceClient produtoServiceClient;

    /**
     * Envia uma operação de estoque para cada item do pedido que possua {@code produtoId}.
     *
     * @param pedido   pedido cujos itens serão processados
     * @param operacao {@link TipoOperacaoEstoque#SAIDA} ao confirmar o pedido,
     *                 {@link TipoOperacaoEstoque#ENTRADA} ao cancelar
     */
    void aplicarOperacaoEstoque(Pedido pedido, TipoOperacaoEstoque operacao) {
        pedido.getItens().stream()
              .filter(i -> i.getProdutoId() != null)
              .forEach(i -> produtoServiceClient.ajustarEstoque(
                  i.getProdutoId(), operacao, i.getQuantidade().inteiro()));
    }
}
