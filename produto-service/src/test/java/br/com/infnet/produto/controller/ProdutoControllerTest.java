package br.com.infnet.produto.controller;

import br.com.infnet.controller.GlobalExceptionHandler;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.exception.ProdutoNaoEncontradoException;
import br.com.infnet.produto.dto.ProdutoResponse;
import br.com.infnet.produto.service.ProdutoService;
import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProdutoController.class, GlobalExceptionHandler.class})
class ProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProdutoService service;

    @Test
    @WithMockUser
    void deveRetornarListaDeProdutosComStatus200() throws Exception {
        when(service.filtrar(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                Produto.novo("Monitor", "MON-001", new BigDecimal("2500.0")),
                Produto.novo("Mouse", "MOU-001", new BigDecimal("150.0"))
        )));

        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/lista"))
                .andExpect(model().attributeExists("produtos"));
    }

    @Test
    @WithMockUser
    void deveExibirFormularioDeNovoProduto() throws Exception {
        mockMvc.perform(get("/produtos/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/form"))
                .andExpect(model().attribute("titulo", "Novo Produto"))
                .andExpect(model().attributeExists("categorias"));
    }

    @ParameterizedTest
    @CsvSource({
        "Monitor 4K,    2500.00",
        "Teclado,        350.00",
        "Mouse Gamer,    199.90"
    })
    @WithMockUser
    void deveCadastrarProdutoERedirecionarParaLista(String nome, BigDecimal preco) throws Exception {
        when(service.cadastrar(eq(nome.trim()), eq(preco), any(), any(), any(), any()))
                .thenReturn(Produto.novo(nome.trim(), "SKU-AUTO", preco));

        mockMvc.perform(post("/produtos")
                        .with(csrf())
                        .param("nome", nome.trim())
                        .param("preco", preco.toPlainString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"));
    }

    @Test
    @WithMockUser
    void deveExibirDetalheComStatus200() throws Exception {
        UUID id = UUID.randomUUID();
        Produto produto = Produto.novo("Monitor", "MON-XXXX", new BigDecimal("2500.0"));
        when(service.buscarPorId(id)).thenReturn(produto);

        mockMvc.perform(get("/produtos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/detalhe"))
                .andExpect(model().attributeExists("produto"));
    }

    @Test
    @WithMockUser
    void deveRedirecionarComErroQuandoDetalheProdutoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenThrow(new ProdutoNaoEncontradoException(id));

        mockMvc.perform(get("/produtos/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"));
    }

    @Test
    @WithMockUser
    void deveExibirFormularioPreenchidoParaEdicao() throws Exception {
        UUID id = UUID.randomUUID();
        Produto produto = Produto.novo("Monitor", "MON-001", new BigDecimal("2500.0"));
        when(service.buscarPorId(id)).thenReturn(produto);

        mockMvc.perform(get("/produtos/{id}/editar", id))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/form"))
                .andExpect(model().attribute("nome", "Monitor"))
                .andExpect(model().attribute("titulo", "Editar Produto"))
                .andExpect(model().attributeExists("categorias"));
    }

    @Test
    @WithMockUser
    void deveAtualizarProdutoERedirecionarParaLista() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.atualizar(eq(id), eq("Monitor Atualizado"), any(BigDecimal.class), any(), any(), any(), any(), any()))
                .thenReturn(Produto.novo("Monitor Atualizado", "MON-ATU-001", new BigDecimal("3000.0")));

        mockMvc.perform(post("/produtos/{id}", id)
                        .with(csrf())
                        .param("nome", "Monitor Atualizado")
                        .param("preco", "3000.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"));
    }

    @Test
    @WithMockUser
    void deveExcluirProdutoERedirecionarParaLista() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).remover(id);

        mockMvc.perform(post("/produtos/{id}/excluir", id)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"));
    }

    @Test
    @WithMockUser
    void deveRedirecionarComErroQuandoProdutoNaoEncontradoNaEdicao() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenThrow(new ProdutoNaoEncontradoException(id));

        mockMvc.perform(get("/produtos/{id}/editar", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"));
    }

    @Test
    @WithMockUser
    void deveExibirFormComErroQuandoCadastrarFalha() throws Exception {
        when(service.cadastrar(anyString(), any(BigDecimal.class), any(), any(), any(), any()))
                .thenThrow(new DomainException("Nome obrigatorio"));

        mockMvc.perform(post("/produtos")
                        .with(csrf())
                        .param("nome", "")
                        .param("preco", "100.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/form"))
                .andExpect(model().attributeExists("erro"));
    }

    @Test
    @WithMockUser
    void deveExibirFormComErroQuandoAtualizarFalha() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.atualizar(eq(id), anyString(), any(BigDecimal.class), any(), any(), any(), any(), any()))
                .thenThrow(new DomainException("Preco deve ser maior que zero"));

        mockMvc.perform(post("/produtos/{id}", id)
                        .with(csrf())
                        .param("nome", "Monitor")
                        .param("preco", "-1.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/form"))
                .andExpect(model().attributeExists("erro"));
    }

    @Test
    @WithMockUser
    void deveRedirecionarComErroQuandoDomainExceptionEscapaDoExcluir() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DomainException("Produto em uso")).when(service).remover(id);

        mockMvc.perform(post("/produtos/{id}/excluir", id)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"));
    }

    @Test
    @WithMockUser
    void deveAtivarPromocaoERedirecionarParaDetalhe() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.ativarPromocao(eq(id), any(), any(), any())).thenReturn(new ProdutoResponse());

        mockMvc.perform(post("/produtos/{id}/promocao", id)
                        .with(csrf())
                        .param("percentual", "20.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos/" + id));
    }

    @Test
    @WithMockUser
    void deveAtivarPromocaoComDatasERedirecionarParaDetalhe() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.ativarPromocao(eq(id), any(), any(), any())).thenReturn(new ProdutoResponse());

        mockMvc.perform(post("/produtos/{id}/promocao", id)
                        .with(csrf())
                        .param("percentual", "15.00")
                        .param("dataInicio", "2026-06-01T10:00")
                        .param("dataFim", "2026-12-31T23:59"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos/" + id));
    }

    @Test
    @WithMockUser
    void deveRedirecionarComErroQuandoAtivarPromocaoFalha() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.ativarPromocao(eq(id), any(), any(), any()))
                .thenThrow(new DomainException("Produto inativo nao pode ter promocao ativada"));

        mockMvc.perform(post("/produtos/{id}/promocao", id)
                        .with(csrf())
                        .param("percentual", "20.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos/" + id));
    }

    @Test
    @WithMockUser
    void deveEncerrarPromocaoERedirecionarParaDetalhe() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.encerrarPromocao(id)).thenReturn(new ProdutoResponse());

        mockMvc.perform(post("/produtos/{id}/promocao/encerrar", id)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos/" + id));
    }

    @Test
    @WithMockUser
    void deveRedirecionarComErroQuandoEncerrarPromocaoFalha() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.encerrarPromocao(id)).thenThrow(new DomainException("Sem promocao ativa"));

        mockMvc.perform(post("/produtos/{id}/promocao/encerrar", id)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos/" + id));
    }
}
