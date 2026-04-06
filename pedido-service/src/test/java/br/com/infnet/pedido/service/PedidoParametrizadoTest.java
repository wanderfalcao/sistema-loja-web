package br.com.infnet.pedido.service;

import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.factory.PedidoTestFactory;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.mapper.PedidoMapper;
import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.pedido.repository.PedidoRepository;
import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes parametrizados do PedidoService.
 * Usa @CsvSource, @ValueSource e @MethodSource para cobrir
 * múltiplos cenários com uma única definição de teste.
 */
@ExtendWith(MockitoExtension.class)
class PedidoParametrizadoTest {

    @Mock
    PedidoRepository repository;

    @Mock
    PedidoMapper mapper;

    @Mock
    ProdutoServiceClient produtoServiceClient;

    @Mock
    EstoqueOrquestrador estoqueOrquestrador;

    @Mock
    StatusHistoricoRegistrador historicoRegistrador;

    @Spy
    PedidoValidador validador;

    @Spy
    PedidoStatusMachine statusMachine;

    @InjectMocks
    PedidoService service;

    @ParameterizedTest(name = "criarComItens: nomeProduto=\"{0}\" precoUnitario={1}")
    @CsvSource({
        "Monitor 4K,          10.00",
        "Teclado Mecânico,     0.01",
        "Mouse Gamer,      99999.99",
        "Café expresso,         3.50",
        "Produto A B C,        50.00",
        "Headset Pro,          20.00"
    })
    void criarComItens_comItensValidos_salvaPedido(String nomeProduto, BigDecimal precoUnitario) {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ItemPedidoRequest item = new ItemPedidoRequest(null, nomeProduto, null, precoUnitario, 1);
        Pedido p = service.criarComItens(List.of(item), null);

        assertThat(p.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(p.getId()).isNotNull();
        assertThat(p.getItens()).hasSize(1);
        assertThat(p.getValor().quantia()).isEqualByComparingTo(precoUnitario);
    }

    // Transições que realmente mudam estado → save deve ser chamado
    @ParameterizedTest(name = "transição real: {0} → {1}")
    @MethodSource("transicoesQueAlteramEstado")
    void avancarStatus_transicoesReais_persisteERetornaNovoStatus(
            StatusPedido inicial, StatusPedido esperado) {

        Pedido pedido = PedidoTestFactory.pedidoCom(inicial);
        UUID id = pedido.getId();
        when(repository.findById(id)).thenReturn(Optional.of(pedido));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Pedido resultado = service.avancarStatus(id, esperado);

        assertThat(resultado.getStatus()).isEqualTo(esperado);
        verify(repository).save(pedido);
    }

    static Stream<Arguments> transicoesQueAlteramEstado() {
        return Stream.of(
            Arguments.of(StatusPedido.PENDENTE,    StatusPedido.CANCELADO),
            Arguments.of(StatusPedido.PROCESSANDO, StatusPedido.CONCLUIDO),
            Arguments.of(StatusPedido.PROCESSANDO, StatusPedido.CANCELADO),
            Arguments.of(StatusPedido.CONCLUIDO,   StatusPedido.CONTESTADO),
            Arguments.of(StatusPedido.CONTESTADO,  StatusPedido.PROCESSANDO),
            Arguments.of(StatusPedido.CONTESTADO,  StatusPedido.CANCELADO)
        );
    }

    // Transições idempotentes → save NÃO deve ser chamado
    @ParameterizedTest(name = "idempotente: {0} → {0}")
    @MethodSource("transicoesIdempotentes")
    void avancarStatus_transicoesIdempotentes_naoSalva(StatusPedido status) {
        Pedido pedido = PedidoTestFactory.pedidoCom(status);
        UUID id = pedido.getId();
        when(repository.findById(id)).thenReturn(Optional.of(pedido));

        Pedido resultado = service.avancarStatus(id, status);

        assertThat(resultado.getStatus()).isEqualTo(status);
        verify(repository, never()).save(any());
    }

    static Stream<Arguments> transicoesIdempotentes() {
        return Stream.of(
            Arguments.of(StatusPedido.PENDENTE),
            Arguments.of(StatusPedido.PROCESSANDO),
            Arguments.of(StatusPedido.CONCLUIDO),
            Arguments.of(StatusPedido.CONTESTADO),
            Arguments.of(StatusPedido.CANCELADO)
        );
    }

    @ParameterizedTest(name = "transição inválida: {0} → {1}")
    @MethodSource("transicoesInvalidas")
    void avancarStatus_transicoesInvalidas_lancaDomainException(
            StatusPedido inicial, StatusPedido tentativa) {

        Pedido pedido = PedidoTestFactory.pedidoCom(inicial);
        UUID id = pedido.getId();
        when(repository.findById(id)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.avancarStatus(id, tentativa))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Transição inválida");
    }

    static Stream<Arguments> transicoesInvalidas() {
        return Stream.of(
            Arguments.of(StatusPedido.PENDENTE,   StatusPedido.CONCLUIDO),
            Arguments.of(StatusPedido.PENDENTE,   StatusPedido.CONTESTADO),
            // CONCLUIDO só pode ir para CONTESTADO
            Arguments.of(StatusPedido.CONCLUIDO,  StatusPedido.PENDENTE),
            Arguments.of(StatusPedido.CONCLUIDO,  StatusPedido.PROCESSANDO),
            Arguments.of(StatusPedido.CONCLUIDO,  StatusPedido.CANCELADO),
            // CONTESTADO não pode voltar para CONCLUIDO nem PENDENTE
            Arguments.of(StatusPedido.CONTESTADO, StatusPedido.CONCLUIDO),
            Arguments.of(StatusPedido.CONTESTADO, StatusPedido.PENDENTE),
            // CANCELADO é terminal
            Arguments.of(StatusPedido.CANCELADO,  StatusPedido.PENDENTE),
            Arguments.of(StatusPedido.CANCELADO,  StatusPedido.PROCESSANDO),
            Arguments.of(StatusPedido.CANCELADO,  StatusPedido.CONCLUIDO),
            Arguments.of(StatusPedido.CANCELADO,  StatusPedido.CONTESTADO)
        );
    }

    @ParameterizedTest(name = "atualizarObservacao: novaObs=\"{0}\"")
    @ValueSource(strings = {"Entregar pela manhã", "Sem urgência", "Urgente!", ""})
    void atualizarObservacao_comDiversasObs_persisteAlteracoes(String novaObs) {
        Pedido pedido = PedidoTestFactory.pedidoCom(StatusPedido.PENDENTE);
        UUID id = pedido.getId();
        when(repository.findById(id)).thenReturn(Optional.of(pedido));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Pedido resultado = service.atualizarObservacao(id, novaObs);

        assertThat(resultado.getObservacao()).isEqualTo(novaObs);
    }
}
