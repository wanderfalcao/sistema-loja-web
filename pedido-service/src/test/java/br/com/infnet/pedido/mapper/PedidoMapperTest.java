package br.com.infnet.pedido.mapper;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.dto.PedidoRequest;
import br.com.infnet.pedido.dto.PedidoResponse;
import br.com.infnet.pedido.factory.PedidoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

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
        assertThat(response.getValor()).isEqualByComparingTo(pedido.getValor());
        assertThat(response.getStatus()).isEqualTo(pedido.getStatus());
        assertThat(response.getObservacao()).isEqualTo(pedido.getObservacao());
        assertThat(response.getDataCriacao()).isEqualTo(pedido.getDataCriacao());
    }

    @Test
    void toResponse_comPedidoNull_retornaNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponseList_mapeiaLista() {
        Pedido p1 = PedidoFactory.criar("Pedido 1", new BigDecimal("10.00"), null);
        Pedido p2 = PedidoFactory.criar("Pedido 2", new BigDecimal("20.00"), null);

        List<PedidoResponse> responses = mapper.toResponseList(List.of(p1, p2));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDescricao()).isEqualTo("Pedido 1");
        assertThat(responses.get(1).getDescricao()).isEqualTo("Pedido 2");
    }

    @Test
    void toResponseList_comListaNull_retornaNull() {
        assertThat(mapper.toResponseList(null)).isNull();
    }

    @Test
    void toResponseList_comListaVazia_retornaListaVazia() {
        assertThat(mapper.toResponseList(List.of())).isEmpty();
    }

    @Test
    void toEntity_mapeiaDescricaoEValor() {
        PedidoRequest request = new PedidoRequest("Pedido via request", new BigDecimal("15.00"), "obs");

        Pedido pedido = mapper.toEntity(request);

        assertThat(pedido.getDescricao()).isEqualTo("Pedido via request");
        assertThat(pedido.getValor()).isEqualByComparingTo("15.00");
        assertThat(pedido.getObservacao()).isEqualTo("obs");
    }

    @Test
    void toEntity_camposIgnorados_ficamNulos() {
        PedidoRequest request = new PedidoRequest("Desc", new BigDecimal("5.00"), null);

        Pedido pedido = mapper.toEntity(request);

        assertThat(pedido.getId()).isNull();
        assertThat(pedido.getStatus()).isNull();
        assertThat(pedido.getDataCriacao()).isNull();
        assertThat(pedido.getDataAtualizacao()).isNull();
    }

    @Test
    void toEntity_comRequestNull_retornaNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_comStatusContestado_mapeiaCorretamente() {
        Pedido pedido = PedidoFactory.criar("Pedido", new BigDecimal("10.00"), "motivo");
        pedido.setStatus(StatusPedido.CONTESTADO);

        PedidoResponse response = mapper.toResponse(pedido);

        assertThat(response.getStatus()).isEqualTo(StatusPedido.CONTESTADO);
        assertThat(response.getObservacao()).isEqualTo("motivo");
    }
}
