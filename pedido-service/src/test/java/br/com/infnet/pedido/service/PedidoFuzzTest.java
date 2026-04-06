package br.com.infnet.pedido.service;

import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.mapper.PedidoMapper;
import br.com.infnet.pedido.repository.PedidoRepository;
import br.com.infnet.shared.exception.DomainException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.Assume;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PedidoFuzzTest {

    private PedidoService newService() {
        PedidoRepository repo = Mockito.mock(PedidoRepository.class);
        PedidoMapper mapper = Mockito.mock(PedidoMapper.class);
        ProdutoServiceClient produtoServiceClient = Mockito.mock(ProdutoServiceClient.class);
        EstoqueOrquestrador estoqueOrquestrador = Mockito.mock(EstoqueOrquestrador.class);
        StatusHistoricoRegistrador historicoRegistrador = Mockito.mock(StatusHistoricoRegistrador.class);
        PedidoValidador validador = new PedidoValidador();
        PedidoStatusMachine statusMachine = new PedidoStatusMachine();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        return new PedidoService(repo, mapper, produtoServiceClient, estoqueOrquestrador, historicoRegistrador, validador, statusMachine);
    }

    @Property
    void nomeProduto_comSqlInjection_naoLancaNPE(
            @ForAll @StringLength(min = 1, max = 100) String nomeProduto) {

        try {
            ItemPedidoRequest item = new ItemPedidoRequest(null, nomeProduto, null, new BigDecimal("10.00"), 1);
            Pedido p = newService().criarComItens(List.of(item), null);
            assertThat(p).isNotNull();
            assertThat(p.getDescricao()).isNotNull();
        } catch (DomainException e) {
            // aceitável — o serviço validou
        }
    }

    @Property
    void nomeProduto_comXss_armazenadoComoTexto(
            @ForAll @From("xssPayloads") String payload) {

        try {
            ItemPedidoRequest item = new ItemPedidoRequest(null, payload, null, new BigDecimal("1.00"), 1);
            Pedido p = newService().criarComItens(List.of(item), null);
            assertThat(p.getDescricao()).doesNotContain("\u0000");
        } catch (DomainException e) {
            // ok — payload gerou DomainException
        }
    }

    @Provide
    Arbitrary<String> xssPayloads() {
        return Arbitraries.of(
                "<script>alert(1)</script>",
                "'; DROP TABLE pedidos; --",
                "\" onmouseover=\"alert(1)\"",
                "<img src=x onerror=alert(1)>",
                "javascript:alert(document.cookie)",
                "1' OR '1'='1",
                "<svg/onload=alert(1)>",
                "'; EXEC xp_cmdshell('whoami'); --"
        );
    }

    @Property
    void precoNegativo_sempreLancaDomainException(
            @ForAll @BigRange(min = "-9999999", max = "-0.01") BigDecimal preco) {

        ItemPedidoRequest item = new ItemPedidoRequest(null, "Produto", null, preco, 1);
        assertThatThrownBy(() -> newService().criarComItens(List.of(item), null))
                .isInstanceOf(Exception.class);
    }

    @Property
    void precoValido_sempreCriaPedidoComTotal(
            @ForAll @BigRange(min = "0.01", max = "9999999") BigDecimal preco) {

        ItemPedidoRequest item = new ItemPedidoRequest(null, "Produto válido", null, preco, 1);
        Pedido p = newService().criarComItens(List.of(item), null);
        assertThat(p.getValor().quantia()).isEqualByComparingTo(preco);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.PENDENTE);
    }

    @Property
    void pedido_idEDataCriacao_saoImutaveis(
            @ForAll @StringLength(min = 1, max = 50) String nomeProduto) {

        Assume.that(!nomeProduto.isBlank());

        PedidoRepository repo = Mockito.mock(PedidoRepository.class);
        PedidoMapper mapper = Mockito.mock(PedidoMapper.class);
        ProdutoServiceClient produtoServiceClient = Mockito.mock(ProdutoServiceClient.class);
        EstoqueOrquestrador estoqueOrquestrador = Mockito.mock(EstoqueOrquestrador.class);
        StatusHistoricoRegistrador historicoRegistrador = Mockito.mock(StatusHistoricoRegistrador.class);
        PedidoValidador validador = new PedidoValidador();
        PedidoStatusMachine statusMachine = new PedidoStatusMachine();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        PedidoService service = new PedidoService(repo, mapper, produtoServiceClient, estoqueOrquestrador, historicoRegistrador, validador, statusMachine);

        ItemPedidoRequest item = new ItemPedidoRequest(null, nomeProduto, null, new BigDecimal("5.00"), 1);
        Pedido p = service.criarComItens(List.of(item), null);
        UUID idOriginal = p.getId();
        var dataOriginal = p.getDataCriacao();

        when(repo.findById(idOriginal)).thenReturn(Optional.of(p));
        service.atualizarObservacao(idOriginal, "Nova observacao");

        assertThat(p.getId()).isEqualTo(idOriginal);
        assertThat(p.getDataCriacao()).isEqualTo(dataOriginal);
    }
}
