package br.com.infnet.pedido.mapper;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.dto.PedidoRequest;
import br.com.infnet.pedido.dto.PedidoResponse;
import br.com.infnet.pedido.factory.PedidoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class PedidoMapperTest {

    PedidoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PedidoMapperImpl();
    }

    @Test
    void toResponse_mapeiaTodasAsPropriedades() {
        Pedido pedido = PedidoFactory.criar("Descrição teste", new BigDecimal("49.90"), "obs");

        PedidoResponse response = mapper.toResponse(pedido);

        assertThat(response.getId()).isEqualTo(pedido.getId());
        assertThat(response.getDescricao()).isEqualTo(pedido.getDescricao());
        assertThat(response.getValor()).isEqualByComparingTo(pedido.getValor().quantia());
        assertThat(response.getStatus()).isEqualTo(pedido.getStatus());
        assertThat(response.getObservacao()).isEqualTo(pedido.getObservacao());
        assertThat(response.getDataCriacao()).isEqualTo(pedido.getDataCriacao());
    }

    @Test
    void toResponse_comPedidoNull_retornaNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void factory_mapeiaDescricaoEValor() {
        PedidoRequest request = new PedidoRequest("Pedido via request", new BigDecimal("15.00"), "obs");

        Pedido pedido = PedidoFactory.criar(request);

        assertThat(pedido.getDescricao()).isEqualTo("Pedido via request");
        assertThat(pedido.getValor().quantia()).isEqualByComparingTo("15.00");
        assertThat(pedido.getObservacao()).isEqualTo("obs");
    }

    @Test
    void factory_pedidoCriado_temIdEStatusPendente() {
        PedidoRequest request = new PedidoRequest("Desc", new BigDecimal("5.00"), null);

        Pedido pedido = PedidoFactory.criar(request);

        assertThat(pedido.getId()).isNotNull();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PENDENTE);
    }

    @Test
    void toResponse_mapeiaPedidoCriadoViaFactory_corretamente() {
        PedidoRequest request = new PedidoRequest("Mapeado", new BigDecimal("7.50"), null);
        Pedido pedido = PedidoFactory.criar(request);

        PedidoResponse response = mapper.toResponse(pedido);

        assertThat(response.getId()).isEqualTo(pedido.getId());
        assertThat(response.getDescricao()).isEqualTo("Mapeado");
        assertThat(response.getValor()).isEqualByComparingTo("7.50");
    }

    @Test
    void toResponse_comStatusContestado_mapeiaCorretamente() {
        Pedido pedido = PedidoFactory.criar("Pedido", new BigDecimal("10.00"), "motivo");
        pedido.avancarStatus(StatusPedido.CONTESTADO);

        PedidoResponse response = mapper.toResponse(pedido);

        assertThat(response.getStatus()).isEqualTo(StatusPedido.CONTESTADO);
        assertThat(response.getObservacao()).isEqualTo("motivo");
    }
}
