package br.com.infnet.pedido.controller;

import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.domain.exception.PedidoNaoEncontradoException;
import br.com.infnet.pedido.dto.ContestarRequest;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.dto.ItemPedidoResponse;
import br.com.infnet.pedido.dto.PedidoRequest;
import br.com.infnet.pedido.dto.PedidoResponse;
import br.com.infnet.pedido.service.PedidoService;
import br.com.infnet.shared.exception.DomainException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import br.com.infnet.controller.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = PedidoRestController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = GlobalExceptionHandler.class
    )
)
class PedidoRestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PedidoService service;

    PedidoResponse response;
    UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        response = PedidoResponse.builder()
                .id(id).descricao("Pedido Teste").valor(new BigDecimal("50.00"))
                .status(StatusPedido.PENDENTE).itens(List.of())
                .valorTotal(new BigDecimal("50.00")).build();
    }

    @Test
    void listar_retornaPageDeRespostas() throws Exception {
        Page<PedidoResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(service.listar(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/pedidos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].descricao").value("Pedido Teste"));
    }

    @Test
    void listar_comPaginacao_parametrosRespeitados() throws Exception {
        Page<PedidoResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(service.listar(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/pedidos?page=0&size=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void buscar_retornaPedidoResponse() throws Exception {
        when(service.buscarDTO(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/pedidos/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void buscar_naoEncontrado_retorna404ComProblemDetail() throws Exception {
        when(service.buscarDTO(id)).thenThrow(new PedidoNaoEncontradoException(id));

        mockMvc.perform(get("/api/v1/pedidos/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void criar_comDadosValidos_retorna201() throws Exception {
        PedidoRequest request = new PedidoRequest("Novo Pedido", new BigDecimal("25.00"), null);
        when(service.criarDTO(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void criar_descricaoVazia_retorna400() throws Exception {
        PedidoRequest request = new PedidoRequest("", new BigDecimal("25.00"), null);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criar_valorAbaixoMinimo_retorna400() throws Exception {
        PedidoRequest request = new PedidoRequest("Desc", new BigDecimal("0.00"), null);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizar_comDadosValidos_retorna200() throws Exception {
        PedidoRequest request = new PedidoRequest("Desc atualizada", new BigDecimal("30.00"), null);
        when(service.atualizarDTO(eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/pedidos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_naoEncontrado_retorna404() throws Exception {
        PedidoRequest request = new PedidoRequest("Desc", new BigDecimal("10.00"), null);
        when(service.atualizarDTO(eq(id), any())).thenThrow(new PedidoNaoEncontradoException(id));

        mockMvc.perform(put("/api/v1/pedidos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void avancarStatus_valido_retorna200() throws Exception {
        PedidoResponse processando = PedidoResponse.builder()
                .id(id).descricao("Pedido Teste").valor(new BigDecimal("50.00"))
                .status(StatusPedido.PROCESSANDO).itens(List.of())
                .valorTotal(new BigDecimal("50.00")).build();
        when(service.avancarStatusDTO(eq(id), eq(StatusPedido.PROCESSANDO))).thenReturn(processando);

        mockMvc.perform(post("/api/v1/pedidos/" + id + "/status")
                        .param("novoStatus", "PROCESSANDO")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSANDO"));
    }

    @Test
    void avancarStatus_transicaoInvalida_retorna422() throws Exception {
        when(service.avancarStatusDTO(eq(id), any()))
                .thenThrow(new DomainException("Transição inválida: PENDENTE → CONCLUIDO"));

        mockMvc.perform(post("/api/v1/pedidos/" + id + "/status")
                        .param("novoStatus", "CONCLUIDO")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void contestar_comMotivo_retorna200() throws Exception {
        ContestarRequest request = new ContestarRequest("Produto com defeito");
        PedidoResponse contestado = PedidoResponse.builder()
                .id(id).descricao("Pedido Teste").valor(new BigDecimal("50.00"))
                .status(StatusPedido.CONTESTADO).observacao("Produto com defeito")
                .itens(List.of()).valorTotal(new BigDecimal("50.00")).build();
        when(service.contestarDTO(eq(id), any())).thenReturn(contestado);

        mockMvc.perform(post("/api/v1/pedidos/" + id + "/contestar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTESTADO"));
    }

    @Test
    void contestar_motivoVazio_retorna400() throws Exception {
        ContestarRequest request = new ContestarRequest("");

        mockMvc.perform(post("/api/v1/pedidos/" + id + "/contestar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletar_retorna204() throws Exception {
        doNothing().when(service).deletar(id);

        mockMvc.perform(delete("/api/v1/pedidos/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletar_naoEncontrado_retorna404() throws Exception {
        doThrow(new PedidoNaoEncontradoException(id)).when(service).deletar(id);

        mockMvc.perform(delete("/api/v1/pedidos/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── Endpoints de itens ────────────────────────────────────────────────────

    @Test
    void adicionarItem_comDadosValidos_retorna200() throws Exception {
        ItemPedidoRequest itemRequest = new ItemPedidoRequest(null, "Monitor 4K", null,
                new BigDecimal("2500.00"), 2);
        when(service.adicionarItem(eq(id), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/pedidos/" + id + "/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void adicionarItem_nomeProdutoVazio_retorna400() throws Exception {
        ItemPedidoRequest itemRequest = new ItemPedidoRequest(null, "", null,
                new BigDecimal("10.00"), 1);

        mockMvc.perform(post("/api/v1/pedidos/" + id + "/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarItens_retornaListaVazia_quandoSemItens() throws Exception {
        when(service.buscarDTO(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/pedidos/" + id + "/itens")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void removerItem_retorna200ComPedidoAtualizado() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(service.removerItem(eq(id), eq(itemId))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/pedidos/" + id + "/itens/" + itemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ── RestExceptionHandler: generic Exception handler ──────────────────────

    @Test
    void listar_genericException_retorna500() throws Exception {
        when(service.listar(any())).thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/api/v1/pedidos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void buscar_genericException_retorna500() throws Exception {
        when(service.buscarDTO(id)).thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/api/v1/pedidos/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
