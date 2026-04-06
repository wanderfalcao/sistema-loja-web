package br.com.infnet.pedido.controller;

import br.com.infnet.controller.GlobalExceptionHandler;
import br.com.infnet.pedido.factory.PedidoTestFactory;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.domain.exception.PedidoNaoEncontradoException;
import br.com.infnet.pedido.service.PedidoService;
import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PedidoController.class, GlobalExceptionHandler.class})
class PedidoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PedidoService service;

    @Test
    void listar_retornaView_comPedidos() throws Exception {
        Page<Pedido> page = new PageImpl<>(List.of(PedidoTestFactory.pedidoPendente()));
        when(service.listarPaginadoComFiltros(any(), any(), any())).thenReturn(page);
        when(service.contarPorStatus()).thenReturn(java.util.Map.of());
        when(service.somarValoresAtivos()).thenReturn(BigDecimal.TEN);

        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(view().name("pedidos/list"))
                .andExpect(model().attributeExists("pedidos", "contagemPorStatus", "totalAtivo"));
    }

    @Test
    void listar_retornaView_semPedidos() throws Exception {
        Page<Pedido> page = new PageImpl<>(List.of());
        when(service.listarPaginadoComFiltros(any(), any(), any())).thenReturn(page);
        when(service.contarPorStatus()).thenReturn(java.util.Map.of());
        when(service.somarValoresAtivos()).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pedidos", List.of()));
    }

    @Test
    void detalhe_retornaDetailView() throws Exception {
        Pedido p = PedidoTestFactory.pedidoPendente();
        when(service.buscar(p.getId())).thenReturn(p);

        mockMvc.perform(get("/pedidos/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pedidos/detail"))
                .andExpect(model().attributeExists("pedido"));
    }

    @Test
    void detalhe_pedidoNaoEncontrado_redirecionaComErro() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscar(id)).thenThrow(new PedidoNaoEncontradoException(id));

        mockMvc.perform(get("/pedidos/" + id))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void formularioNovo_retornaFormView() throws Exception {
        mockMvc.perform(get("/pedidos/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("pedidos/form"));
    }

    @Test
    void criar_comDadosValidos_redireciona() throws Exception {
        when(service.criar(any(), any(), any())).thenReturn(PedidoTestFactory.pedidoPendente());

        mockMvc.perform(post("/pedidos")
                        .param("descricao", "Meu pedido")
                        .param("valor", "25.00")
                        .param("observacao", "Entregar pela manhã"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/pedidos/*"));
    }

    @Test
    void criar_semObservacao_redireciona() throws Exception {
        when(service.criar(any(), any(), any())).thenReturn(PedidoTestFactory.pedidoPendente());

        mockMvc.perform(post("/pedidos")
                        .param("descricao", "Pedido sem obs")
                        .param("valor", "10.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/pedidos/*"));
    }

    @Test
    void criar_comValorInvalido_redirecionaComErro() throws Exception {
        mockMvc.perform(post("/pedidos")
                        .param("descricao", "Test")
                        .param("valor", "abc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void criar_comValorVazio_redirecionaComErro() throws Exception {
        mockMvc.perform(post("/pedidos")
                        .param("descricao", "Test")
                        .param("valor", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void criar_domainExceptionNoService_redirecionaComErro() throws Exception {
        when(service.criar(any(), any(), any()))
                .thenThrow(new DomainException("Valor inválido"));

        mockMvc.perform(post("/pedidos")
                        .param("descricao", "X")
                        .param("valor", "0.001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void formularioEditar_retornaFormComPedido() throws Exception {
        Pedido p = PedidoTestFactory.pedidoPendente();
        when(service.buscar(p.getId())).thenReturn(p);

        mockMvc.perform(get("/pedidos/" + p.getId() + "/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("pedidos/form"))
                .andExpect(model().attributeExists("pedido"));
    }

    @Test
    void formularioEditar_pedidoNaoEncontrado_redirecionaComErro() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscar(id)).thenThrow(new PedidoNaoEncontradoException(id));

        mockMvc.perform(get("/pedidos/" + id + "/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void atualizar_comDadosValidos_redireciona() throws Exception {
        Pedido p = PedidoTestFactory.pedidoPendente();
        when(service.atualizar(eq(p.getId()), any(), any(), any())).thenReturn(p);

        mockMvc.perform(post("/pedidos/" + p.getId())
                        .param("descricao", "Novo nome")
                        .param("valor", "20.00")
                        .param("observacao", "obs atualizada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos"));
    }

    @Test
    void avancarStatus_valido_redireciona() throws Exception {
        Pedido p = PedidoTestFactory.pedidoPendente();
        when(service.avancarStatus(eq(p.getId()), eq(StatusPedido.PROCESSANDO))).thenReturn(p);

        mockMvc.perform(post("/pedidos/" + p.getId() + "/status")
                        .param("novoStatus", "PROCESSANDO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos"));
    }

    @Test
    void avancarStatus_transicaoInvalida_redirecionaComErro() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.avancarStatus(eq(id), any()))
                .thenThrow(new DomainException("Transição inválida"));

        mockMvc.perform(post("/pedidos/" + id + "/status")
                        .param("novoStatus", "CONCLUIDO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void contestar_valido_redireciona() throws Exception {
        Pedido p = PedidoTestFactory.pedidoPendente();
        when(service.contestar(eq(p.getId()), any())).thenReturn(p);

        mockMvc.perform(post("/pedidos/" + p.getId() + "/contestar")
                        .param("motivo", "Produto com defeito"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos"));
    }

    @Test
    void contestar_domainException_redirecionaComErro() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.contestar(eq(id), any()))
                .thenThrow(new DomainException("Transição inválida"));

        mockMvc.perform(post("/pedidos/" + id + "/contestar")
                        .param("motivo", "algum motivo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void deletar_redireciona() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).deletar(id);

        mockMvc.perform(post("/pedidos/" + id + "/deletar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos"));
    }

    @Test
    void deletar_naoEncontrado_redirecionaComErro() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new br.com.infnet.pedido.domain.exception.PedidoNaoEncontradoException(id))
                .when(service).deletar(id);

        mockMvc.perform(post("/pedidos/" + id + "/deletar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void atualizar_comValorInvalido_redirecionaComErro() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/pedidos/" + id)
                        .param("descricao", "Teste")
                        .param("valor", "nao-numerico"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    // ── GlobalExceptionHandler: null-message DomainException branch ──────────

    @Test
    void criar_domainExceptionComMensagemNula_redirecionaComErroGenerico() throws Exception {
        when(service.criar(any(), any(), any())).thenThrow(new DomainException(null));

        mockMvc.perform(post("/pedidos")
                        .param("descricao", "Teste")
                        .param("valor", "10.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    // ── GlobalExceptionHandler: generic Exception handler ────────────────────

    @Test
    void listar_genericException_redirecionaComErro() throws Exception {
        when(service.listarPaginadoComFiltros(any(), any(), any())).thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/pedidos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void adicionarItem_comPrecoValido_redirecionaParaDetalhe() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.adicionarItem(eq(id), any())).thenReturn(null);

        mockMvc.perform(post("/pedidos/" + id + "/itens")
                        .param("nomeProduto", "Monitor 4K")
                        .param("precoUnitario", "2500.00")
                        .param("quantidade", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos/" + id));
    }

    @Test
    void adicionarItem_semPrecoUnitario_redirecionaParaDetalhe() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.adicionarItem(eq(id), any())).thenReturn(null);

        mockMvc.perform(post("/pedidos/" + id + "/itens")
                        .param("nomeProduto", "Produto Manual")
                        .param("quantidade", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos/" + id));
    }

    @Test
    void removerItem_redirecionaParaDetalhe() throws Exception {
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(service.removerItem(eq(id), eq(itemId))).thenReturn(null);

        mockMvc.perform(post("/pedidos/" + id + "/itens/" + itemId + "/remover"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos/" + id));
    }
}
