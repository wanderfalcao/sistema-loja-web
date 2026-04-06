package br.com.infnet.pedido.service;

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

    @ParameterizedTest(name = "criar pedido: desc=\"{0}\" valor={1}")
    @CsvSource({
        "Pedido simples,          10.00",
        "Pedido com vírgula,       0.01",
        "Pedido valor alto,    99999.99",
        "Café expresso,            3.50",
        "Produto A B C,           50.00",
        "  Espaços nas bordas  ,  20.00"
    })
    void criar_comDadosValidos_salvaPedido(String descricao, BigDecimal valor) {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Pedido p = service.criar(descricao, valor);

        assertThat(p.getDescricao()).isNotBlank();
        assertThat(p.getValor().quantia()).isEqualByComparingTo(valor);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(p.getId()).isNotNull();
    }

    @ParameterizedTest(name = "criar com descrição inválida: \"{0}\"")
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void criar_comDescricaoEmBranco_lancaDomainException(String descricao) {
        assertThatThrownBy(() -> service.criar(descricao, new BigDecimal("10.00")))
                .isInstanceOf(DomainException.class);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest(name = "criar com valor inválido: {0}")
    @ValueSource(strings = {"0.00", "-0.01", "-100", "-9999999.99"})
    void criar_comValorAbaixoDoMinimo_lancaDomainException(String valorStr) {
        BigDecimal valor = new BigDecimal(valorStr);
        assertThatThrownBy(() -> service.criar("Desc válida", valor))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no mínimo");
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

    @ParameterizedTest(name = "atualizar: nova desc=\"{0}\" novo valor={1}")
    @CsvSource({
        "Nova descrição,    15.00",
        "Atualizado,         0.01",
        "Outro nome,     1000.00"
    })
    void atualizar_comDadosValidos_persisteAlteracoes(String novaDesc, BigDecimal novoValor) {
        Pedido pedido = PedidoTestFactory.pedidoCom(StatusPedido.PENDENTE);
        UUID id = pedido.getId();
        when(repository.findById(id)).thenReturn(Optional.of(pedido));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Pedido resultado = service.atualizar(id, novaDesc, novoValor, null);

        assertThat(resultado.getDescricao()).isEqualTo(novaDesc.trim());
        assertThat(resultado.getValor().quantia()).isEqualByComparingTo(novoValor);
    }

    @ParameterizedTest(name = "atualizar com descrição inválida: \"{0}\"")
    @ValueSource(strings = {"", "   ", "\t"})
    void atualizar_comDescricaoEmBranco_lancaDomainException(String descricao) {
        assertThatThrownBy(() -> service.atualizar(UUID.randomUUID(), descricao, new BigDecimal("10.00"), null))
                .isInstanceOf(DomainException.class);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest(name = "atualizar com valor inválido: {0}")
    @ValueSource(strings = {"0.00", "-0.01", "-50.00"})
    void atualizar_comValorInvalido_lancaDomainException(String valorStr) {
        assertThatThrownBy(() -> service.atualizar(UUID.randomUUID(), "Desc", new BigDecimal(valorStr), null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no mínimo");
        verify(repository, never()).save(any());
    }
}
