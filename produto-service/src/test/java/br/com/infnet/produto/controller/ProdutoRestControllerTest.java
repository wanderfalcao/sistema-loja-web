package br.com.infnet.produto.controller;

import br.com.infnet.controller.RestExceptionHandler;
import br.com.infnet.produto.domain.TipoOperacaoEstoque;
import br.com.infnet.produto.domain.exception.ProdutoNaoEncontradoException;
import br.com.infnet.produto.dto.AjusteEstoqueRequest;
import br.com.infnet.produto.dto.ProdutoRequest;
import br.com.infnet.produto.dto.ProdutoResponse;
import br.com.infnet.produto.dto.PromocaoRequest;
import br.com.infnet.produto.service.ProdutoService;
import br.com.infnet.shared.exception.DomainException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProdutoRestController.class, RestExceptionHandler.class})
class ProdutoRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProdutoService service;

    // ── GET /api/v1/produtos ──────────────────────────────────────────────────

    @Test
    void deveListarProdutosComStatus200() throws Exception {
        ProdutoResponse r1 = buildResponse(UUID.randomUUID(), "Monitor 4K", "MON-4K-001", new BigDecimal("2500.00"));
        ProdutoResponse r2 = buildResponse(UUID.randomUUID(), "Mouse Gamer", "MOU-GAM-001", new BigDecimal("150.00"));

        when(service.listar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r1, r2)));

        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].nome").value("Monitor 4K"));
    }

    // ── GET /api/v1/produtos/{id} ─────────────────────────────────────────────

    @Test
    void deveBuscarProdutoPorIdComStatus200() throws Exception {
        UUID id = UUID.randomUUID();
        ProdutoResponse response = buildResponse(id, "Monitor 4K", "MON-4K-001", new BigDecimal("2500.00"));
        when(service.buscarDTO(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/produtos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Monitor 4K"))
                .andExpect(jsonPath("$.sku").value("MON-4K-001"));
    }

    @Test
    void deveDevolverProblemDetail404QuandoProdutoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarDTO(id)).thenThrow(new ProdutoNaoEncontradoException(id));

        mockMvc.perform(get("/api/v1/produtos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    // ── GET /api/v1/produtos/sku/{sku} ────────────────────────────────────────

    @Test
    void deveBuscarProdutoPorSkuComStatus200() throws Exception {
        String sku = "MON-4K-ABCD";
        ProdutoResponse response = buildResponse(UUID.randomUUID(), "Monitor 4K", sku, new BigDecimal("2500.00"));
        when(service.buscarPorSku(sku)).thenReturn(response);

        mockMvc.perform(get("/api/v1/produtos/sku/{sku}", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.nome").value("Monitor 4K"));
    }

    @Test
    void deveDevolver422QuandoSkuNaoEncontrado() throws Exception {
        String sku = "SKU-INVALIDO";
        when(service.buscarPorSku(sku)).thenThrow(new DomainException("Produto não encontrado para SKU: " + sku));

        mockMvc.perform(get("/api/v1/produtos/sku/{sku}", sku))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    // ── POST /api/v1/produtos ─────────────────────────────────────────────────

    @Test
    void deveCriarProdutoValido201() throws Exception {
        ProdutoRequest request = new ProdutoRequest(
                "Monitor 4K", "Monitor 4K UHD",
                new BigDecimal("2500.00"), 10, true, null, null, null);
        UUID id = UUID.randomUUID();
        ProdutoResponse response = buildResponse(id, "Monitor 4K", "MON-4K-001", new BigDecimal("2500.00"));
        when(service.criarDTO(any(ProdutoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Monitor 4K"))
                .andExpect(header().exists("Location"));
    }

    @Test
    void deveDevolver400QuandoRequestInvalido() throws Exception {
        String invalidJson = """
                {"nome":"","preco":null,"estoque":0}
                """;

        mockMvc.perform(post("/api/v1/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    // ── PUT /api/v1/produtos/{id} ─────────────────────────────────────────────

    @Test
    void deveAtualizarProdutoComStatus200() throws Exception {
        UUID id = UUID.randomUUID();
        ProdutoRequest request = new ProdutoRequest(
                "Monitor 4K Pro", null,
                new BigDecimal("3200.00"), 5, true, null, null, null);
        ProdutoResponse response = buildResponse(id, "Monitor 4K Pro", "MON-4K-PRO", new BigDecimal("3200.00"));
        when(service.atualizarDTO(eq(id), any(ProdutoRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/produtos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Monitor 4K Pro"));
    }

    // ── DELETE /api/v1/produtos/{id} ──────────────────────────────────────────

    @Test
    void deveRemoverProdutoComStatus204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).remover(id);

        mockMvc.perform(delete("/api/v1/produtos/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).remover(id);
    }

    // ── PATCH /api/v1/produtos/{id}/estoque ───────────────────────────────────

    @Test
    void deveAjustarEstoqueComStatus200() throws Exception {
        UUID id = UUID.randomUUID();
        AjusteEstoqueRequest ajuste = new AjusteEstoqueRequest(TipoOperacaoEstoque.ENTRADA, 50);
        ProdutoResponse response = buildResponse(id, "Monitor 4K", "MON-4K-001", new BigDecimal("2500.00"));
        response.setEstoque(60);
        when(service.ajustarEstoque(eq(id), eq(TipoOperacaoEstoque.ENTRADA), eq(50))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/produtos/{id}/estoque", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ajuste)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estoque").value(60));
    }

    @Test
    void deveDevolver422QuandoDomainException() throws Exception {
        UUID id = UUID.randomUUID();
        AjusteEstoqueRequest ajuste = new AjusteEstoqueRequest(TipoOperacaoEstoque.SAIDA, 100);
        when(service.ajustarEstoque(eq(id), eq(TipoOperacaoEstoque.SAIDA), eq(100)))
                .thenThrow(new DomainException("Estoque insuficiente. Disponivel: 5"));

        mockMvc.perform(patch("/api/v1/produtos/{id}/estoque", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ajuste)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void deveDevolver500QuandoExcecaoGenerica() throws Exception {
        when(service.listar(any(Pageable.class)))
                .thenThrow(new RuntimeException("erro inesperado de infraestrutura"));

        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"));
    }

    // ── PATCH /api/v1/produtos/{id}/promocao ──────────────────────────────────

    @Test
    void deveAtivarPromocaoComStatus200() throws Exception {
        UUID id = UUID.randomUUID();
        PromocaoRequest request = new PromocaoRequest(new java.math.BigDecimal("20"), null, null);
        ProdutoResponse response = buildResponse(id, "Monitor 4K", "MON-4K-001", new BigDecimal("2500.00"));
        response.setPrecoComDesconto(new BigDecimal("2000.00"));
        when(service.ativarPromocao(eq(id), any(), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/produtos/{id}/promocao", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoComDesconto").value(2000.00));
    }

    @Test
    void deveEncerrarPromocaoRestComStatus200() throws Exception {
        UUID id = UUID.randomUUID();
        ProdutoResponse response = buildResponse(id, "Monitor 4K", "MON-4K-001", new BigDecimal("2500.00"));
        when(service.encerrarPromocao(id)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/produtos/{id}/promocao", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Monitor 4K"));
    }

    @Test
    void deveDevolver400QuandoPercentualNulo() throws Exception {
        UUID id = UUID.randomUUID();
        String invalidJson = """
                {"percentualDesconto":null}
                """;

        mockMvc.perform(patch("/api/v1/produtos/{id}/promocao", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ProdutoResponse buildResponse(UUID id, String nome, String sku, BigDecimal preco) {
        ProdutoResponse r = new ProdutoResponse();
        r.setId(id);
        r.setNome(nome);
        r.setSku(sku);
        r.setPreco(preco);
        r.setEstoque(10);
        r.setAtivo(true);
        return r;
    }
}
