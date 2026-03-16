package br.com.infnet.config;

import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final PedidoRepository pedidoRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (pedidoRepository.count() > 0) {
            log.info("DataLoader: banco já possui dados — seed ignorado.");
            return;
        }

        log.info("DataLoader: inserindo pedidos de exemplo...");

        // Pedido 1 — PENDENTE com itens (snapshots sem vínculo a produto-service em seed)
        Pedido p1 = Pedido.novo("Pedido loja online", new BigDecimal("748.80"), null);
        ItemPedido i1a = item(p1, "Teclado Mecânico RGB", "TEC-MEC-RGB", new BigDecimal("349.00"), 1);
        ItemPedido i1b = item(p1, "Mouse Gamer 16000 DPI", "MOU-GAM-16K", new BigDecimal("199.90"), 2);
        p1.setItens(new ArrayList<>(List.of(i1a, i1b)));
        pedidoRepository.save(p1);

        // Pedido 2 — PROCESSANDO
        Pedido p2 = Pedido.novo("Compra de acessórios", new BigDecimal("89.90"), "Entrega expressa");
        p2.avancarStatus(StatusPedido.PROCESSANDO);
        ItemPedido i2 = item(p2, "Webcam Full HD 1080p", "WEB-FHD-108", new BigDecimal("89.90"), 1);
        p2.setItens(new ArrayList<>(List.of(i2)));
        pedidoRepository.save(p2);

        // Pedido 3 — CANCELADO
        Pedido p3 = Pedido.novo("Pedido cancelado", new BigDecimal("199.90"), null);
        p3.avancarStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(p3);

        log.info("DataLoader: {} pedidos inseridos.", pedidoRepository.count());
    }

    private ItemPedido item(Pedido pedido, String nome, String sku,
                             BigDecimal preco, int quantidade) {
        ItemPedido item = new ItemPedido();
        item.setId(UUID.randomUUID());
        item.setPedido(pedido);
        item.setNomeProduto(nome);
        item.setSkuProduto(sku);
        item.setPrecoUnitario(preco);
        item.setQuantidade(quantidade);
        item.setSubtotal(preco.multiply(BigDecimal.valueOf(quantidade)));
        return item;
    }
}
