package br.com.infnet.pedido.domain;

import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.factory.ItemPedidoFactory;
import br.com.infnet.pedido.factory.PedidoFactory;
import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ItemPedidoTest {

    Pedido pedido;

    @BeforeEach
    void setUp() {
        pedido = PedidoFactory.criar("Pedido Teste", new BigDecimal("10.00"), null);
    }

    @Test
    void criarItem_comDadosValidos_deveRetornarItemCorreto() {
        ItemPedidoRequest request = new ItemPedidoRequest(null, "Monitor 4K", "MON-001",
                new BigDecimal("2500.00"), 2);

        ItemPedido item = ItemPedidoFactory.criar(pedido, request);

        assertThat(item.getId()).isNotNull();
        assertThat(item.getNomeProduto()).isEqualTo("Monitor 4K");
        assertThat(item.getSkuProduto()).isEqualTo("MON-001");
        assertThat(item.getPrecoUnitario()).isEqualByComparingTo("2500.00");
        assertThat(item.getQuantidade()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualByComparingTo("5000.00");
        assertThat(item.getPedido()).isEqualTo(pedido);
    }

    @Test
    void criarItem_subtotalDeveSerPrecoPorQuantidade() {
        ItemPedidoRequest request = new ItemPedidoRequest(null, "Mouse", null,
                new BigDecimal("199.90"), 3);

        ItemPedido item = ItemPedidoFactory.criar(pedido, request);

        assertThat(item.getSubtotal()).isEqualByComparingTo("599.70");
    }

    @Test
    void criarItem_quantidadeMenorQueUm_deveLancarDomainException() {
        ItemPedidoRequest request = new ItemPedidoRequest(null, "Teclado", null,
                new BigDecimal("300.00"), 0);

        assertThatThrownBy(() -> ItemPedidoFactory.criar(pedido, request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("1");
    }

    @Test
    void criarItem_quantidadeNula_deveLancarDomainException() {
        ItemPedidoRequest request = new ItemPedidoRequest(null, "Teclado", null,
                new BigDecimal("300.00"), null);

        assertThatThrownBy(() -> ItemPedidoFactory.criar(pedido, request))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void criarItem_precoNegativo_deveLancarDomainException() {
        ItemPedidoRequest request = new ItemPedidoRequest(null, "Headset", null,
                new BigDecimal("-10.00"), 1);

        assertThatThrownBy(() -> ItemPedidoFactory.criar(pedido, request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("zero");
    }

    @Test
    void criarItem_precoZero_deveLancarDomainException() {
        ItemPedidoRequest request = new ItemPedidoRequest(null, "Headset", null,
                BigDecimal.ZERO, 1);

        assertThatThrownBy(() -> ItemPedidoFactory.criar(pedido, request))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void criarItem_semSku_deveTerSkuNulo() {
        ItemPedidoRequest request = new ItemPedidoRequest(null, "Webcam", null,
                new BigDecimal("89.90"), 1);

        ItemPedido item = ItemPedidoFactory.criar(pedido, request);

        assertThat(item.getSkuProduto()).isNull();
    }

    @Test
    void pedidoComItens_calcularTotal_deveSomarSubtotais() {
        ItemPedidoRequest r1 = new ItemPedidoRequest(null, "Monitor", null, new BigDecimal("2500.00"), 2);
        ItemPedidoRequest r2 = new ItemPedidoRequest(null, "Mouse", null, new BigDecimal("199.00"), 1);

        pedido.getItens().add(ItemPedidoFactory.criar(pedido, r1));
        pedido.getItens().add(ItemPedidoFactory.criar(pedido, r2));

        // 2500*2 + 199*1 = 5199
        assertThat(pedido.calcularTotal()).isEqualByComparingTo("5199.00");
    }

    @Test
    void pedidoSemItens_calcularTotal_deveRetornarValorManual() {
        assertThat(pedido.calcularTotal()).isEqualByComparingTo(pedido.getValor());
    }
}
