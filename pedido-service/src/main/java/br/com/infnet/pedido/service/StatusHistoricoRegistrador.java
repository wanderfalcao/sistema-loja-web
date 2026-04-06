package br.com.infnet.pedido.service;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusHistorico;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.repository.StatusHistoricoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Persiste e consulta o histórico de transições de status de um pedido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class StatusHistoricoRegistrador {

    private final StatusHistoricoRepository statusHistoricoRepository;

    /**
     * Registra uma transição de status, criando um novo {@link StatusHistorico}.
     *
     * @param pedido   pedido que sofreu a transição
     * @param anterior status anterior ao da transição
     * @param novo     novo status após a transição
     * @param motivo   motivo opcional da transição (usado em contestações)
     */
    void registrar(Pedido pedido, StatusPedido anterior, StatusPedido novo, String motivo) {
        statusHistoricoRepository.save(new StatusHistorico(pedido, anterior, novo, motivo));
        log.info("Histórico de status registrado: pedido={} {} → {}", pedido.getId(), anterior, novo);
    }

    /**
     * Retorna o histórico de transições de um pedido em ordem cronológica crescente.
     *
     * @param pedidoId identificador do pedido
     * @return lista de {@link StatusHistorico}, do mais antigo ao mais recente
     */
    List<StatusHistorico> buscarHistorico(UUID pedidoId) {
        return statusHistoricoRepository.findAllByPedidoIdOrderByDataTransicaoAsc(pedidoId);
    }
}
