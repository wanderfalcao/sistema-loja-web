package br.com.infnet.pedido.service;

import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.client.TipoOperacaoEstoque;
import br.com.infnet.pedido.domain.Dinheiro;
import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.factory.ItemPedidoFactory;
import br.com.infnet.pedido.factory.PedidoFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstoqueOrquestradorTest {

    @Mock
    ProdutoServiceClient produtoServiceClient;

    @InjectMocks
    EstoqueOrquestrador orquestrador;

    @Test
    void aplicarOperacaoEstoque_comItemSemProdutoId_naoChama() {
        Pedido pedido = PedidoFactory.criar("Pedido", new BigDecimal("10.00"), null);
        ItemPedidoRequest req = new ItemPedidoRequest(null, "Mouse", null, new BigDecimal("50.00"), 1);
        pedido.getItens().add(ItemPedidoFactory.criar(pedido, req));

        orquestrador.aplicarOperacaoEstoque(pedido, TipoOperacaoEstoque.SAIDA);

        verifyNoInteractions(produtoServiceClient);
    }

    @Test
    void aplicarOperacaoEstoque_comItemComProdutoId_chamaClient() {
        UUID produtoId = UUID.randomUUID();
        Pedido pedido = PedidoFactory.criar("Pedido", new BigDecimal("10.00"), null);
        ItemPedidoRequest req = new ItemPedidoRequest(produtoId, "Monitor", "MON-001",
                new BigDecimal("1000.00"), 2);
        pedido.getItens().add(ItemPedidoFactory.criar(pedido, req));

        orquestrador.aplicarOperacaoEstoque(pedido, TipoOperacaoEstoque.SAIDA);

        verify(produtoServiceClient).ajustarEstoque(produtoId, TipoOperacaoEstoque.SAIDA, 2);
    }

    @Test
    void aplicarOperacaoEstoque_pedidoSemItens_naoChama() {
        Pedido pedido = PedidoFactory.criar("Pedido", new BigDecimal("10.00"), null);

        orquestrador.aplicarOperacaoEstoque(pedido, TipoOperacaoEstoque.ENTRADA);

        verifyNoInteractions(produtoServiceClient);
    }
}
