package br.com.infnet.pedido.service;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.domain.exception.PedidoNaoEncontradoException;
import br.com.infnet.pedido.dto.ContestarRequest;
import br.com.infnet.pedido.dto.PedidoRequest;
import br.com.infnet.pedido.dto.PedidoResponse;
import br.com.infnet.pedido.factory.PedidoTestFactory;
import br.com.infnet.pedido.mapper.PedidoMapper;
import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.pedido.repository.PedidoRepository;
import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceDTOTest {

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

    Pedido pedido;
    PedidoResponse responseDTO;
    UUID id;

    @BeforeEach
    void setUp() {
        pedido = PedidoTestFactory.pedidoPendente();
        id = pedido.getId();
        responseDTO = PedidoResponse.builder()
                .id(id).descricao("Test").valor(new BigDecimal("10.00"))
                .status(StatusPedido.PENDENTE).itens(List.of())
                .valorTotal(new BigDecimal("10.00")).build();
    }

    @Test
    void listarPageable_retornaPageDeResponses() {
        Page<Pedido> page = new PageImpl<>(List.of(pedido), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);
        when(mapper.toResponse(pedido)).thenReturn(responseDTO);

        Page<PedidoResponse> resultado = service.listar(PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0)).isEqualTo(responseDTO);
    }

    @Test
    void listarPageable_vazio_retornaPageVazia() {
        Page<Pedido> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<PedidoResponse> resultado = service.listar(PageRequest.of(0, 10));

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }

    @Test
    void buscarDTO_retornaPedidoResponse() {
        when(repository.findById(id)).thenReturn(Optional.of(pedido));
        when(mapper.toResponse(pedido)).thenReturn(responseDTO);

        PedidoResponse resultado = service.buscarDTO(id);

        assertThat(resultado).isEqualTo(responseDTO);
    }

    @Test
    void buscarDTO_naoEncontrado_lancaExcecao() {
        UUID inexistente = UUID.randomUUID();
        when(repository.findById(inexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarDTO(inexistente))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }

    @Test
    void criarDTO_comDadosValidos_retornaPedidoResponse() {
        PedidoRequest request = new PedidoRequest("Novo Pedido", new BigDecimal("25.00"), "obs");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(responseDTO);

        PedidoResponse resultado = service.criarDTO(request);

        assertThat(resultado).isEqualTo(responseDTO);
        verify(repository).save(any(Pedido.class));
    }

    @Test
    void criarDTO_descricaoVazia_lancaDomainException() {
        PedidoRequest request = new PedidoRequest("", new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> service.criarDTO(request))
                .isInstanceOf(DomainException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void criarDTO_valorAbaixoMinimo_lancaDomainException() {
        PedidoRequest request = new PedidoRequest("Ok", BigDecimal.ZERO, null);

        assertThatThrownBy(() -> service.criarDTO(request))
                .isInstanceOf(DomainException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarDTO_comDadosValidos_retornaPedidoResponse() {
        PedidoRequest request = new PedidoRequest("Nova Desc", new BigDecimal("30.00"), null);
        when(repository.findById(id)).thenReturn(Optional.of(pedido));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(responseDTO);

        PedidoResponse resultado = service.atualizarDTO(id, request);

        assertThat(resultado).isEqualTo(responseDTO);
    }

    @Test
    void atualizarDTO_pedidoNaoEncontrado_lancaExcecao() {
        PedidoRequest request = new PedidoRequest("Desc", new BigDecimal("10.00"), null);
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizarDTO(id, request))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }

    @Test
    void avancarStatusDTO_retornaPedidoResponseComNovoStatus() {
        when(repository.findById(id)).thenReturn(Optional.of(pedido));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        PedidoResponse processando = PedidoResponse.builder()
                .id(id).descricao("Test").valor(new BigDecimal("10.00"))
                .status(StatusPedido.PROCESSANDO).itens(List.of())
                .valorTotal(new BigDecimal("10.00")).build();
        when(mapper.toResponse(any())).thenReturn(processando);

        // PENDENTE → CANCELADO is valid without items
        PedidoResponse resultado = service.avancarStatusDTO(id, StatusPedido.CANCELADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.PROCESSANDO);
    }

    @Test
    void contestarDTO_comMotivo_retornaPedidoResponseContestado() {
        pedido.avancarStatus(StatusPedido.CONCLUIDO);
        ContestarRequest request = new ContestarRequest("Produto com defeito");
        when(repository.findById(id)).thenReturn(Optional.of(pedido));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        PedidoResponse contestado = PedidoResponse.builder()
                .id(id).descricao("Test").valor(new BigDecimal("10.00"))
                .status(StatusPedido.CONTESTADO).observacao("Produto com defeito")
                .itens(List.of()).valorTotal(new BigDecimal("10.00")).build();
        when(mapper.toResponse(any())).thenReturn(contestado);

        PedidoResponse resultado = service.contestarDTO(id, request);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.CONTESTADO);
    }

    @Test
    void contestarDTO_motivoVazio_lancaDomainException() {
        ContestarRequest request = new ContestarRequest("");

        assertThatThrownBy(() -> service.contestarDTO(id, request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Motivo");
    }
}
