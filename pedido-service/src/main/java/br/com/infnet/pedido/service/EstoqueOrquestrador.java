package br.com.infnet.pedido.service;

import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.client.TipoOperacaoEstoque;
import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordena os ajustes de estoque junto ao {@code produto-service} para cada item de um pedido.
 * Ignora itens sem {@code produtoId}, pois esses não possuem referência rastreável de estoque.
 *
 * <p>Em caso de falha durante o processamento, os itens já ajustados são compensados
 * com a operação inversa (padrão Saga). Se a compensação também falhar, o erro é
 * registrado em log para intervenção manual, pois não há transação distribuída nativa
 * entre microsserviços.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class EstoqueOrquestrador {

    private final ProdutoServiceClient produtoServiceClient;

    /**
     * Envia uma operação de estoque para cada item do pedido que possua {@code produtoId}.
     * Se qualquer item falhar após os retries configurados em {@code ProdutoServiceClient},
     * os itens já processados são revertidos com a operação inversa.
     *
     * @param pedido   pedido cujos itens serão processados
     * @param operacao {@link TipoOperacaoEstoque#SAIDA} ao confirmar o pedido,
     *                 {@link TipoOperacaoEstoque#ENTRADA} ao cancelar
     */
    void aplicarOperacaoEstoque(Pedido pedido, TipoOperacaoEstoque operacao) {
        TipoOperacaoEstoque inverso = (operacao == TipoOperacaoEstoque.SAIDA)
                ? TipoOperacaoEstoque.ENTRADA
                : TipoOperacaoEstoque.SAIDA;

        List<ItemPedido> rastreavéis = pedido.getItens().stream()
                .filter(i -> i.getProdutoId() != null)
                .toList();

        List<ItemPedido> processados = new ArrayList<>();

        for (ItemPedido item : rastreavéis) {
            try {
                produtoServiceClient.ajustarEstoque(item.getProdutoId(), operacao, item.getQuantidade().inteiro());
                processados.add(item);
            } catch (Exception ex) {
                log.error("Falha ao ajustar estoque do item {} (op={}) após retries — compensando {} itens já processados",
                        item.getProdutoId(), operacao, processados.size(), ex);
                compensar(processados, inverso);
                throw new DomainException("Falha ao ajustar estoque. Operação revertida.");
            }
        }
    }

    /**
     * Aplica a operação inversa nos itens já processados para desfazer ajustes parciais.
     * Falhas na compensação são registradas em log — não lançam exceção para não mascarar
     * o erro original, mas exigem intervenção manual para corrigir inconsistências.
     */
    private void compensar(List<ItemPedido> itens, TipoOperacaoEstoque operacao) {
        itens.forEach(item -> {
            try {
                produtoServiceClient.ajustarEstoque(item.getProdutoId(), operacao, item.getQuantidade().inteiro());
                log.info("Compensação aplicada com sucesso para item {}", item.getProdutoId());
            } catch (Exception ex) {
                log.error("COMPENSAÇÃO FALHOU para item {} — inconsistência de estoque requer intervenção manual",
                        item.getProdutoId(), ex);
            }
        });
    }
}
