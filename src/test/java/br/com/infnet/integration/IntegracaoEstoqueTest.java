package br.com.infnet.integration;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.service.PedidoService;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.SkuGenerator;
import br.com.infnet.produto.repository.ProdutoRepository;
import br.com.infnet.produto.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class IntegracaoEstoqueTest {

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    void aoProcessarPedido_deveDebitarEstoqueDoProduto() {
        Produto produto = Produto.novo("Produto Integração", SkuGenerator.fromNome("Prod Integr"), new BigDecimal("100.00"));
        produto.setEstoque(10);
        produtoRepository.save(produto);

        Pedido pedido = pedidoService.criar("Pedido integração", new BigDecimal("100.00"), null);

        ItemPedidoRequest itemReq = new ItemPedidoRequest(
                produto.getId(), produto.getNome(), produto.getSku(),
                new BigDecimal("100.00"), 1);
        pedidoService.adicionarItem(pedido.getId(), itemReq);

        pedidoService.avancarStatus(pedido.getId(), StatusPedido.PROCESSANDO);

        Produto atualizado = produtoService.buscarPorId(produto.getId());
        assertThat(atualizado.getEstoque()).isEqualTo(9);
    }

    @Test
    void aoCancelarPedidoProcessando_deveRestaurarEstoque() {
        Produto produto = Produto.novo("Produto Devolucao", SkuGenerator.fromNome("Prod Devolucao"), new BigDecimal("50.00"));
        produto.setEstoque(10);
        produtoRepository.save(produto);

        Pedido pedido = pedidoService.criar("Pedido devolução", new BigDecimal("50.00"), null);

        ItemPedidoRequest itemReq = new ItemPedidoRequest(
                produto.getId(), produto.getNome(), produto.getSku(),
                new BigDecimal("50.00"), 2);
        pedidoService.adicionarItem(pedido.getId(), itemReq);
        pedidoService.avancarStatus(pedido.getId(), StatusPedido.PROCESSANDO);

        pedidoService.avancarStatus(pedido.getId(), StatusPedido.CANCELADO);

        Produto atualizado = produtoService.buscarPorId(produto.getId());
        assertThat(atualizado.getEstoque()).isEqualTo(10);
    }
}
