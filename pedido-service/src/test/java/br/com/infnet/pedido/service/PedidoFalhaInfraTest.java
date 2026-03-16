package br.com.infnet.pedido.service;

import br.com.infnet.pedido.factory.PedidoTestFactory;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.exception.PedidoNaoEncontradoException;
import br.com.infnet.pedido.mapper.PedidoMapper;
import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoFalhaInfraTest {

    @Mock
    PedidoRepository repository;

    @Mock
    PedidoMapper mapper;

    @Mock
    ProdutoServiceClient produtoServiceClient;

    @InjectMocks
    PedidoService service;

    @Test
    void listar_timeoutNoBanco_propagaQueryTimeoutException() {
        when(repository.findAllByOrderByDataCriacaoDesc())
                .thenThrow(new QueryTimeoutException("Timeout ao listar pedidos"));

        assertThatThrownBy(() -> service.listar())
                .isInstanceOf(QueryTimeoutException.class)
                .hasMessageContaining("Timeout");
    }

    @Test
    void buscar_timeoutNoBanco_propagaQueryTimeoutException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id))
                .thenThrow(new QueryTimeoutException("Timeout ao buscar pedido"));

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(QueryTimeoutException.class);
    }

    @Test
    void criar_timeoutAoSalvar_propagaQueryTimeoutException() {
        when(repository.save(any()))
                .thenThrow(new QueryTimeoutException("Timeout ao salvar"));

        assertThatThrownBy(() -> service.criar("Pedido", new BigDecimal("10.00")))
                .isInstanceOf(QueryTimeoutException.class);
    }

    @Test
    void listar_bancoIndisponivel_propagaDataAccessException() {
        when(repository.findAllByOrderByDataCriacaoDesc())
                .thenThrow(new TransientDataAccessResourceException("Banco indisponível"));

        assertThatThrownBy(() -> service.listar())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void criar_bancoIndisponivel_naoSilenciaExcecao() {
        when(repository.save(any()))
                .thenThrow(new TransientDataAccessResourceException("Conexão recusada"));

        assertThatThrownBy(() -> service.criar("Desc", new BigDecimal("5.00")))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Conexão recusada");
    }

    @Test
    void deletar_bancoIndisponivel_propagaDataAccessException() {
        Pedido pedido = PedidoTestFactory.pedidoPendente();
        UUID id = pedido.getId();
        when(repository.findById(id)).thenReturn(Optional.of(pedido));
        doThrow(new TransientDataAccessResourceException("Banco fora do ar"))
                .when(repository).delete(pedido);

        assertThatThrownBy(() -> service.deletar(id))
                .isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest(name = "buscar ID inexistente #{index}: {0}")
    @ValueSource(strings = {
        "00000000-0000-0000-0000-000000000001",
        "ffffffff-ffff-ffff-ffff-ffffffffffff",
        "12345678-1234-1234-1234-123456789abc"
    })
    void buscar_idInexistente_lancaPedidoNaoEncontrado(String uuidStr) {
        UUID id = UUID.fromString(uuidStr);
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(PedidoNaoEncontradoException.class)
                .hasMessageContaining(uuidStr);
    }

    @Test
    void criar_multiplosRequestsConsecutivos_todosSalvos() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        int total = 50;
        for (int i = 1; i <= total; i++) {
            Pedido p = service.criar("Pedido " + i, new BigDecimal(i + ".00"));
            assertThat(p.getId()).isNotNull();
            assertThat(p.getDescricao()).isEqualTo("Pedido " + i);
        }

        verify(repository, times(total)).save(any(Pedido.class));
    }

    @Test
    void listar_sobrecarga_repositorioChamadoCadaVez() {
        when(repository.findAllByOrderByDataCriacaoDesc()).thenReturn(java.util.List.of());

        int chamadas = 20;
        for (int i = 0; i < chamadas; i++) {
            service.listar();
        }

        verify(repository, times(chamadas)).findAllByOrderByDataCriacaoDesc();
    }

    @Test
    void listar_erroInesperado_propagaRuntimeException() {
        when(repository.findAllByOrderByDataCriacaoDesc())
                .thenThrow(new RuntimeException("Erro inesperado no banco"));

        assertThatThrownBy(() -> service.listar())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro inesperado");
    }
}
