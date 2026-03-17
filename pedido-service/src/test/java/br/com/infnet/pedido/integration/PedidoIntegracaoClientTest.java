package br.com.infnet.pedido.integration;

import br.com.infnet.client.ProdutoInfo;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.dto.PedidoResponse;
import br.com.infnet.pedido.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração que verificam a comunicação real (HTTP) entre
 * pedido-service e produto-service usando WireMock como servidor HTTP fake.
 *
 * <p>A porta 9999 corresponde ao {@code produto.service.url=http://localhost:9999}
 * definido em {@code application-test.properties}.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integração pedido-service → produto-service (WireMock)")
class PedidoIntegracaoClientTest {

    // Porta fixa 9999 alinhada com application-test.properties
    static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(9999));
        wireMockServer.start();
    }

    @AfterAll
    static void pararWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetarStubs() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("Deve debitar estoque no produto-service ao processar pedido com itens")
    void deveDebitarEstoqueAoProcessarPedido() throws Exception {
        UUID produtoId = UUID.randomUUID();

        ProdutoInfo produtoInfo = new ProdutoInfo();
        produtoInfo.setId(produtoId);
        produtoInfo.setNome("Teclado Mecânico");
        produtoInfo.setSku("TEC-001");
        produtoInfo.setPreco(new BigDecimal("350.00"));
        produtoInfo.setAtivo(true);
        produtoInfo.setEstoque(10);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/produtos/" + produtoId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(produtoInfo))));

        wireMockServer.stubFor(patch(urlEqualTo("/api/v1/produtos/" + produtoId + "/estoque"))
                .willReturn(aResponse().withStatus(200)));

        Pedido pedido = pedidoService.criar("Pedido de integração", new BigDecimal("350.00"), null);
        ItemPedidoRequest item = new ItemPedidoRequest();
        item.setProdutoId(produtoId);
        item.setQuantidade(2);
        pedidoService.adicionarItem(pedido.getId(), item);

        Pedido resultado = pedidoService.avancarStatus(pedido.getId(), StatusPedido.PROCESSANDO);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.PROCESSANDO);
        wireMockServer.verify(1,
                patchRequestedFor(urlEqualTo("/api/v1/produtos/" + produtoId + "/estoque")));
    }

    @Test
    @DisplayName("Deve estornar estoque no produto-service ao cancelar pedido em processamento")
    void deveDevolverEstoqueAoCancelarPedidoProcessando() throws Exception {
        UUID produtoId = UUID.randomUUID();

        ProdutoInfo produtoInfo = new ProdutoInfo();
        produtoInfo.setId(produtoId);
        produtoInfo.setNome("Mouse Gamer");
        produtoInfo.setSku("MOU-002");
        produtoInfo.setPreco(new BigDecimal("200.00"));
        produtoInfo.setAtivo(true);
        produtoInfo.setEstoque(5);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/produtos/" + produtoId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(produtoInfo))));

        wireMockServer.stubFor(patch(urlEqualTo("/api/v1/produtos/" + produtoId + "/estoque"))
                .willReturn(aResponse().withStatus(200)));

        Pedido pedido = pedidoService.criar("Pedido para cancelar", new BigDecimal("200.00"), null);
        ItemPedidoRequest item = new ItemPedidoRequest();
        item.setProdutoId(produtoId);
        item.setQuantidade(1);
        pedidoService.adicionarItem(pedido.getId(), item);
        pedidoService.avancarStatus(pedido.getId(), StatusPedido.PROCESSANDO);

        Pedido resultado = pedidoService.avancarStatus(pedido.getId(), StatusPedido.CANCELADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        // 1x SAIDA (ao processar) + 1x ENTRADA (ao cancelar) = 2 chamadas ao /estoque
        wireMockServer.verify(2,
                patchRequestedFor(urlEqualTo("/api/v1/produtos/" + produtoId + "/estoque")));
    }

    @Test
    @DisplayName("Deve enriquecer item com dados reais do produto-service ao adicionar ao pedido")
    void deveEnriquecerItemComDadosDoProdutoService() throws Exception {
        UUID produtoId = UUID.randomUUID();

        ProdutoInfo produtoInfo = new ProdutoInfo();
        produtoInfo.setId(produtoId);
        produtoInfo.setNome("Monitor 4K");
        produtoInfo.setSku("MON-4K");
        produtoInfo.setPreco(new BigDecimal("1500.00"));
        produtoInfo.setAtivo(true);
        produtoInfo.setEstoque(3);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/produtos/" + produtoId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(produtoInfo))));

        Pedido pedido = pedidoService.criar("Pedido monitor", new BigDecimal("1500.00"), null);
        ItemPedidoRequest item = new ItemPedidoRequest();
        item.setProdutoId(produtoId);
        item.setQuantidade(1);

        PedidoResponse response = pedidoService.adicionarItem(pedido.getId(), item);

        assertThat(response.getItens()).hasSize(1);
        assertThat(response.getItens().get(0).getNomeProduto()).isEqualTo("Monitor 4K");
        assertThat(response.getItens().get(0).getSkuProduto()).isEqualTo("MON-4K");
    }
}
