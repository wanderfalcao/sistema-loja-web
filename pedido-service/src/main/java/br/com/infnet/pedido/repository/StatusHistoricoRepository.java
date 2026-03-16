package br.com.infnet.pedido.repository;

import br.com.infnet.pedido.domain.StatusHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StatusHistoricoRepository extends JpaRepository<StatusHistorico, UUID> {

    List<StatusHistorico> findAllByPedidoIdOrderByDataTransicaoAsc(UUID pedidoId);
}
