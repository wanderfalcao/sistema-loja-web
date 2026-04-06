package br.com.infnet.pedido.repository;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findAllByOrderByDataCriacaoDesc();

    Page<Pedido> findAll(Pageable pageable);

    long countByStatus(StatusPedido status);

    @Query("SELECT SUM(p.valor.quantia) FROM Pedido p WHERE p.status <> br.com.infnet.pedido.domain.StatusPedido.CANCELADO")
    java.math.BigDecimal somarValoresAtivos();

    Page<Pedido> findAllByStatus(StatusPedido status, Pageable pageable);

    @Query("SELECT p FROM Pedido p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "LOWER(p.descricao) LIKE CONCAT('%', :descricao, '%')")
    Page<Pedido> filtrarComDescricao(@Param("status") StatusPedido status,
                                     @Param("descricao") String descricao,
                                     Pageable pageable);
}
