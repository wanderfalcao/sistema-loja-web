package br.com.infnet.config;

import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.repository.PedidoRepository;
import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.SkuGenerator;
import br.com.infnet.produto.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (produtoRepository.count() > 0) {
            log.info("DataLoader: banco já possui dados — seed ignorado.");
            return;
        }

        log.info("DataLoader: inserindo dados de exemplo...");

        // ── Produtos ──────────────────────────────────────────────
        Produto monitor = produto("Monitor 4K UHD 27\"", new BigDecimal("2499.90"), 8, CategoriaProduto.MONITORES);
        monitor.ativarPromocao(new BigDecimal("15"), LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        produtoRepository.save(monitor);

        Produto teclado = produtoRepository.save(produto("Teclado Mecânico RGB", new BigDecimal("349.00"), 15, CategoriaProduto.PERIFERICOS));
        Produto mouse   = produtoRepository.save(produto("Mouse Gamer 16000 DPI", new BigDecimal("199.90"), 20, CategoriaProduto.PERIFERICOS));

        Produto headset = produto("Headset Surround 7.1", new BigDecimal("279.00"), 12, CategoriaProduto.AUDIO_VIDEO);
        headset.ativarPromocao(new BigDecimal("10"), null, null);
        produtoRepository.save(headset);

        Produto webcam = produtoRepository.save(produto("Webcam Full HD 1080p", new BigDecimal("89.90"), 30, CategoriaProduto.PERIFERICOS));
        produtoRepository.save(produto("SSD NVMe 1TB",          new BigDecimal("449.00"), 18, CategoriaProduto.ARMAZENAMENTO));
        produtoRepository.save(produto("Memória RAM DDR5 32GB", new BigDecimal("589.90"), 10, CategoriaProduto.COMPONENTES));
        produtoRepository.save(produto("Placa de Vídeo RTX",    new BigDecimal("3799.00"), 5, CategoriaProduto.COMPONENTES));

        // ── Pedidos ───────────────────────────────────────────────
        // Pedido 1 - PENDENTE com itens vinculados a produtos
        Pedido p1 = Pedido.novo("Pedido loja online", new BigDecimal("748.80"), null);
        ItemPedido i1a = new ItemPedido();
        i1a.setPedido(p1);
        i1a.setProdutoId(teclado.getId());
        i1a.setNomeProduto(teclado.getNome());
        i1a.setSkuProduto(teclado.getSku());
        i1a.setPrecoUnitario(teclado.getPreco());
        i1a.setQuantidade(1);
        i1a.setSubtotal(teclado.getPreco());
        ItemPedido i1b = new ItemPedido();
        i1b.setPedido(p1);
        i1b.setProdutoId(mouse.getId());
        i1b.setNomeProduto(mouse.getNome());
        i1b.setSkuProduto(mouse.getSku());
        i1b.setPrecoUnitario(mouse.getPreco());
        i1b.setQuantidade(2);
        i1b.setSubtotal(mouse.getPreco().multiply(new BigDecimal("2")));
        p1.setItens(new java.util.ArrayList<>(List.of(i1a, i1b)));
        pedidoRepository.save(p1);

        // Pedido 2 - PROCESSANDO
        Pedido p2 = Pedido.novo("Compra de acessórios", new BigDecimal("89.90"), "Entrega expressa");
        p2.avancarStatus(StatusPedido.PROCESSANDO);
        ItemPedido i2 = new ItemPedido();
        i2.setPedido(p2);
        i2.setProdutoId(webcam.getId());
        i2.setNomeProduto(webcam.getNome());
        i2.setSkuProduto(webcam.getSku());
        i2.setPrecoUnitario(webcam.getPreco());
        i2.setQuantidade(1);
        i2.setSubtotal(webcam.getPreco());
        p2.setItens(new java.util.ArrayList<>(List.of(i2)));
        pedidoRepository.save(p2);

        // Pedido 3 - CANCELADO simples
        Pedido p3 = Pedido.novo("Pedido cancelado", new BigDecimal("199.90"), null);
        p3.avancarStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(p3);

        log.info("DataLoader: {} produtos e {} pedidos inseridos.",
                produtoRepository.count(), pedidoRepository.count());
    }

    private Produto produto(String nome, BigDecimal preco, int estoque, CategoriaProduto categoria) {
        Produto p = Produto.novo(nome, SkuGenerator.fromNome(nome), preco);
        p.setEstoque(estoque);
        p.setCategoria(categoria);
        return p;
    }
}
