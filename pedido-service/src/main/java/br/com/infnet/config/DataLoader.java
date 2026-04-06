package br.com.infnet.config;

import br.com.infnet.pedido.domain.Dinheiro;
import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.Quantidade;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

        log.info("DataLoader: inserindo pedidos de exemplo da TechStore...");

        // ── PENDENTE — aguardando confirmação ────────────────────────────────
        Pedido montagem = Pedido.novo("Montagem de PC Gamer", new BigDecimal("6588.80"), null);
        montagem.getItens().add(item(montagem, "Placa de Vídeo RTX 4060 ASUS Dual",   "VGA-ASUS-RTX4060", new BigDecimal("3799.00"), 1));
        montagem.getItens().add(item(montagem, "Processador Intel Core i7-14700K",     "CPU-INT-I7-14700", new BigDecimal("2199.00"), 1));
        montagem.getItens().add(item(montagem, "Memória RAM DDR5 32GB Kingston Fury",  "RAM-KNG-32G-DDR5", new BigDecimal("589.90"), 1));
        pedidoRepository.save(montagem);

        Pedido homeOffice = Pedido.novo("Setup Home Office", new BigDecimal("1298.70"), "Entrega em até 3 dias úteis");
        homeOffice.getItens().add(item(homeOffice, "Teclado Mecânico RGB HyperX Alloy FPS", "TEC-HX-MECH-RGB", new BigDecimal("349.00"), 1));
        homeOffice.getItens().add(item(homeOffice, "Mouse Gamer 16000 DPI Razer DeathAdder", "MOU-RZR-16K-V3", new BigDecimal("299.90"), 1));
        homeOffice.getItens().add(item(homeOffice, "Webcam Full HD 1080p Logitech C920",     "WEB-LOG-C920",   new BigDecimal("149.90"), 1));
        homeOffice.getItens().add(item(homeOffice, "Headset Gamer SteelSeries Arctis 7+",    "HST-STS-7PLUS",  new BigDecimal("599.00"), 1));
        pedidoRepository.save(homeOffice);

        // ── PROCESSANDO — pagamento aprovado, separando estoque ───────────────
        Pedido blackFriday = Pedido.novo("Compra Black Friday", new BigDecimal("3448.80"), null);
        blackFriday.getItens().add(item(blackFriday, "Monitor 4K UHD 27\" LG 27UK850",    "MON-LG-4K-27",    new BigDecimal("2499.90"), 1));
        blackFriday.getItens().add(item(blackFriday, "SSD NVMe 1TB Samsung 990 Pro",       "SSD-SAM-1TB-990", new BigDecimal("449.00"),  2));
        blackFriday.avancarStatus(StatusPedido.PROCESSANDO);
        pedidoRepository.save(blackFriday);

        Pedido setupCompleto = Pedido.novo("Setup Completo Streamer", new BigDecimal("2777.80"), "Presente de aniversário — embalar");
        setupCompleto.getItens().add(item(setupCompleto, "Monitor Gamer 144Hz 24\" AOC G2490VX",      "MON-AOC-144-24",  new BigDecimal("1199.00"), 1));
        setupCompleto.getItens().add(item(setupCompleto, "Teclado Mecânico RGB HyperX Alloy FPS",     "TEC-HX-MECH-RGB", new BigDecimal("349.00"),  1));
        setupCompleto.getItens().add(item(setupCompleto, "Mouse Gamer 16000 DPI Razer DeathAdder V3", "MOU-RZR-16K-V3",  new BigDecimal("299.90"),  1));
        setupCompleto.getItens().add(item(setupCompleto, "Headset Gamer SteelSeries Arctis 7+",       "HST-STS-7PLUS",   new BigDecimal("599.00"),  1));
        setupCompleto.getItens().add(item(setupCompleto, "Mousepad Gamer XL Speed Redragon Flick",    "MPD-RED-FLICK-XL",new BigDecimal("79.90"),   1));
        setupCompleto.avancarStatus(StatusPedido.PROCESSANDO);
        pedidoRepository.save(setupCompleto);

        // ── CONCLUÍDO — entregue e confirmado ────────────────────────────────
        Pedido entregue = Pedido.novo("Upgrade de Armazenamento", new BigDecimal("728.90"), null);
        entregue.getItens().add(item(entregue, "SSD NVMe 1TB Samsung 990 Pro",   "SSD-SAM-1TB-990", new BigDecimal("449.00"), 1));
        entregue.getItens().add(item(entregue, "HD Externo 2TB Seagate Expansion","HDE-SEA-2TB-EXP", new BigDecimal("279.90"), 1));
        entregue.avancarStatus(StatusPedido.PROCESSANDO);
        entregue.avancarStatus(StatusPedido.CONCLUIDO);
        pedidoRepository.save(entregue);

        // ── CONTESTADO — cliente abriu disputa após recebimento ───────────────
        Pedido contestado = Pedido.novo("Compra de Monitor", new BigDecimal("1199.00"), null);
        contestado.getItens().add(item(contestado, "Monitor Gamer 144Hz 24\" AOC G2490VX", "MON-AOC-144-24", new BigDecimal("1199.00"), 1));
        contestado.avancarStatus(StatusPedido.PROCESSANDO);
        contestado.avancarStatus(StatusPedido.CONCLUIDO);
        contestado.contestar("Monitor chegou com a tela arranhada e o lacre da caixa violado.");
        pedidoRepository.save(contestado);

        // ── CANCELADO — desistência antes do envio ────────────────────────────
        Pedido canceladoCliente = Pedido.novo("Compra de Periféricos", new BigDecimal("448.90"), null);
        canceladoCliente.getItens().add(item(canceladoCliente, "Teclado Mecânico RGB HyperX Alloy FPS", "TEC-HX-MECH-RGB", new BigDecimal("349.00"), 1));
        canceladoCliente.getItens().add(item(canceladoCliente, "Mousepad Gamer XL Speed Redragon",      "MPD-RED-FLICK-XL", new BigDecimal("79.90"), 1));
        canceladoCliente.avancarStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(canceladoCliente);

        Pedido canceladoEstoque = Pedido.novo("Placa de Vídeo High-End", new BigDecimal("3799.00"), null);
        canceladoEstoque.getItens().add(item(canceladoEstoque, "Placa de Vídeo RTX 4060 ASUS Dual", "VGA-ASUS-RTX4060", new BigDecimal("3799.00"), 1));
        canceladoEstoque.avancarStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(canceladoEstoque);

        log.info("DataLoader: {} pedidos inseridos (cobrindo todos os status: PENDENTE, PROCESSANDO, CONCLUIDO, CONTESTADO, CANCELADO).",
            pedidoRepository.count());
    }

    private ItemPedido item(Pedido pedido, String nome, String sku,
                             BigDecimal preco, int quantidade) {
        return ItemPedido.criar(
            pedido,
            (UUID) null,
            nome,
            sku,
            Dinheiro.dePositivo(preco),
            Quantidade.dePositivo(quantidade)
        );
    }
}
