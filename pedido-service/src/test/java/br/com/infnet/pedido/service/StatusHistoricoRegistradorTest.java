package br.com.infnet.pedido.service;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusHistorico;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.factory.PedidoFactory;
import br.com.infnet.pedido.repository.StatusHistoricoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusHistoricoRegistradorTest {

    @Mock
    StatusHistoricoRepository statusHistoricoRepository;

    @InjectMocks
    StatusHistoricoRegistrador registrador;

    @Test
    void registrar_salvaNoBanco() {
        Pedido pedido = PedidoFactory.criar("Pedido", new BigDecimal("10.00"), null);

        registrador.registrar(pedido, StatusPedido.PENDENTE, StatusPedido.PROCESSANDO, null);

        verify(statusHistoricoRepository).save(any(StatusHistorico.class));
    }

    @Test
    void buscarHistorico_delegaAoRepositorio() {
        UUID pedidoId = UUID.randomUUID();
        when(statusHistoricoRepository.findAllByPedidoIdOrderByDataTransicaoAsc(pedidoId))
                .thenReturn(List.of());

        List<StatusHistorico> resultado = registrador.buscarHistorico(pedidoId);

        assertThat(resultado).isEmpty();
        verify(statusHistoricoRepository).findAllByPedidoIdOrderByDataTransicaoAsc(pedidoId);
    }
}
