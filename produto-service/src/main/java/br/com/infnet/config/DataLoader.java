package br.com.infnet.config;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.Quantidade;
import br.com.infnet.produto.domain.Sku;
import br.com.infnet.produto.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final ProdutoRepository produtoRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (produtoRepository.count() > 0) {
            log.info("DataLoader: banco já possui dados — seed ignorado.");
            return;
        }

        log.info("DataLoader: inserindo catálogo da TechStore...");

        // ── MONITORES (desconto máximo 30%) ──────────────────────────────────
        Produto monitor4k = produto(
            "Monitor 4K UHD 27\" LG 27UK850",
            "MON-LG-4K-27", new BigDecimal("2499.90"), 8, 2,
            CategoriaProduto.MONITORES,
            "Display IPS 4K com USB-C e HDR10. Ideal para produtores de conteúdo e designers.",
            "https://assets.techstore.com/monitores/lg-27uk850.jpg"
        );
        // promoção com datas — 15% dentro do limite de 30%
        monitor4k.ativarPromocao(
            new BigDecimal("15"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(7)
        );
        produtoRepository.save(monitor4k);

        produtoRepository.save(produto(
            "Monitor Gamer 144Hz 24\" AOC G2490VX",
            "MON-AOC-144-24", new BigDecimal("1199.00"), 5, 1,
            CategoriaProduto.MONITORES,
            "Painel VA com 1ms MPRT e AMD FreeSync Premium. Sem tearing em jogos competitivos.",
            null
        ));

        // ── PERIFÉRICOS (desconto máximo 40%) ────────────────────────────────
        produtoRepository.save(produto(
            "Teclado Mecânico RGB HyperX Alloy FPS",
            "TEC-HX-MECH-RGB", new BigDecimal("349.00"), 15, 3,
            CategoriaProduto.PERIFERICOS,
            "Switches Cherry MX Red, NKRO completo e iluminação RGB por tecla.",
            "https://assets.techstore.com/perifericos/hyperx-alloy.jpg"
        ));

        Produto mouseGamer = produto(
            "Mouse Gamer 16000 DPI Razer DeathAdder V3",
            "MOU-RZR-16K-V3", new BigDecimal("299.90"), 20, 5,
            CategoriaProduto.PERIFERICOS,
            "Sensor Focus Pro 30K, formato ergonômico e apenas 59g. Referência em FPS.",
            "https://assets.techstore.com/perifericos/razer-deathadder-v3.jpg"
        );
        // promoção sem datas — válida indefinidamente — 20% dentro de 40%
        mouseGamer.ativarPromocao(new BigDecimal("20"), null, null);
        produtoRepository.save(mouseGamer);

        produtoRepository.save(produto(
            "Webcam Full HD 1080p Logitech C920",
            "WEB-LOG-C920", new BigDecimal("149.90"), 30, 5,
            CategoriaProduto.PERIFERICOS,
            "Resolução Full HD a 30fps com autofoco e microfone estéreo integrado.",
            null
        ));

        // ── ARMAZENAMENTO (desconto máximo 25%) ──────────────────────────────
        Produto ssd = produto(
            "SSD NVMe 1TB Samsung 990 Pro",
            "SSD-SAM-1TB-990", new BigDecimal("449.00"), 18, 3,
            CategoriaProduto.ARMAZENAMENTO,
            "Velocidade de leitura sequencial de 7.450 MB/s. Interface PCIe 4.0 x4.",
            "https://assets.techstore.com/armazenamento/samsung-990pro.jpg"
        );
        // promoção com datas — 10% dentro do limite de 25%
        ssd.ativarPromocao(
            new BigDecimal("10"),
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().plusDays(3)
        );
        produtoRepository.save(ssd);

        produtoRepository.save(produto(
            "HD Externo 2TB Seagate Expansion",
            "HDE-SEA-2TB-EXP", new BigDecimal("279.90"), 12, 2,
            CategoriaProduto.ARMAZENAMENTO,
            "Backup portátil plug-and-play com USB 3.0. Compatível com Windows e Mac.",
            null
        ));

        // ── COMPONENTES (desconto máximo 20%) ────────────────────────────────
        produtoRepository.save(produto(
            "Memória RAM DDR5 32GB Kingston Fury Beast",
            "RAM-KNG-32G-DDR5", new BigDecimal("589.90"), 10, 2,
            CategoriaProduto.COMPONENTES,
            "Kit 2x16GB DDR5-5200MHz CL40 com XMP 3.0 e dissipador de alumínio.",
            "https://assets.techstore.com/componentes/kingston-fury-beast.jpg"
        ));

        Produto rtx = produto(
            "Placa de Vídeo RTX 4060 ASUS Dual",
            "VGA-ASUS-RTX4060", new BigDecimal("3799.00"), 4, 1,
            CategoriaProduto.COMPONENTES,
            "8GB GDDR6, DLSS 3, Ray Tracing. Ideal para 1080p e 1440p ultra.",
            "https://assets.techstore.com/componentes/asus-rtx4060.jpg"
        );
        // promoção sem datas — 15% dentro do limite de 20%
        rtx.ativarPromocao(new BigDecimal("15"), null, null);
        produtoRepository.save(rtx);

        produtoRepository.save(produto(
            "Processador Intel Core i7-14700K",
            "CPU-INT-I7-14700", new BigDecimal("2199.00"), 6, 1,
            CategoriaProduto.COMPONENTES,
            "20 núcleos (8P+12E), boost até 5.6GHz, sem cooler incluso. Socket LGA1700.",
            null
        ));

        // ── ÁUDIO E VÍDEO (desconto máximo 35%) ──────────────────────────────
        Produto headset = produto(
            "Headset Gamer Surround 7.1 SteelSeries Arctis 7+",
            "HST-STS-7PLUS", new BigDecimal("599.00"), 12, 2,
            CategoriaProduto.AUDIO_VIDEO,
            "Sem fio 2.4GHz, bateria de 30h, microfone ClearCast retrátil e certificação Discord.",
            "https://assets.techstore.com/audio/steelseries-arctis7plus.jpg"
        );
        // promoção com datas — 25% dentro do limite de 35%
        headset.ativarPromocao(
            new BigDecimal("25"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(14)
        );
        produtoRepository.save(headset);

        produtoRepository.save(produto(
            "Caixa de Som Bluetooth JBL Flip 6",
            "SPK-JBL-FLIP6", new BigDecimal("599.00"), 8, 1,
            CategoriaProduto.AUDIO_VIDEO,
            "20W RMS, resistência à água IPX7, PartyBoost para conectar múltiplas caixas.",
            null
        ));

        // ── GERAL (desconto máximo 50%) ───────────────────────────────────────
        Produto mousepad = produto(
            "Mousepad Gamer XL Speed Redragon Flick",
            "MPD-RED-FLICK-XL", new BigDecimal("79.90"), 40, 10,
            CategoriaProduto.GERAL,
            "900x400mm, superfície de tecido otimizada para velocidade, base antiderrapante.",
            null
        );
        // promoção sem datas — 40% dentro do limite de 50%
        mousepad.ativarPromocao(new BigDecimal("40"), null, null);
        produtoRepository.save(mousepad);

        produtoRepository.save(produto(
            "Suporte para Monitor Ergonômico Articulado",
            "SUP-MON-ERG-01", new BigDecimal("149.90"), 25, 5,
            CategoriaProduto.GERAL,
            "Articulação completa (pan, tilt, rotação 360°). Suporta monitores de 13\" a 32\" até 9kg.",
            null
        ));

        // produto inativo — estoque zerado abaixo do mínimo configurado
        Produto estabilizador = produto(
            "Estabilizador SMS Net Station 2000VA",
            "EST-SMS-2000VA", new BigDecimal("459.00"), 0, 2,
            CategoriaProduto.GERAL,
            "10 tomadas filtradas, proteção contra sobretensão e display digital. Bivolt automático.",
            null
        );
        estabilizador.desativar();
        produtoRepository.save(estabilizador);

        log.info("DataLoader: {} produtos inseridos.", produtoRepository.count());
    }

    private Produto produto(String nome, String skuStr, BigDecimal preco,
                             int estoque, int estoqueMinimo,
                             CategoriaProduto categoria, String descricao, String imagemUrl) {
        Produto p = Produto.novo(nome, Sku.de(skuStr), preco);
        p.definirEstoque(Quantidade.de(estoque));
        p.definirEstoqueMinimo(Quantidade.de(estoqueMinimo));
        p.definirCategoria(categoria);
        if (descricao != null) p.definirDescricao(descricao);
        if (imagemUrl  != null) p.definirImagemUrl(imagemUrl);
        return p;
    }
}
