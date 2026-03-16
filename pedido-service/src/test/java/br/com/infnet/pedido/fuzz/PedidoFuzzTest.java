package br.com.infnet.pedido.fuzz;

import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.mapper.PedidoMapper;
import br.com.infnet.pedido.repository.PedidoRepository;
import br.com.infnet.pedido.repository.StatusHistoricoRepository;
import br.com.infnet.pedido.service.PedidoService;
import br.com.infnet.shared.exception.DomainException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;

import java.math.BigDecimal;
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
        StatusHistoricoRepository statusRepo = Mockito.mock(StatusHistoricoRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        return new PedidoService(repo, mapper, produtoServiceClient, statusRepo);
    }

    @Property
    void descricao_comSqlInjection_naoLancaNPE(
            @ForAll @StringLength(min = 1, max = Pedido.MAX_DESCRICAO) String descricao) {

        try {
            Pedido p = newService().criar(descricao, new BigDecimal("10.00"));
            assertThat(p).isNotNull();
            assertThat(p.getDescricao()).isNotNull();
        } catch (DomainException e) {
            // aceitável — o serviço validou
        }
    }

    @Property
    void descricao_comXss_armazenadaComoTexto(
            @ForAll @From("xssPayloads") String payload) {

        try {
            Pedido p = newService().criar(payload, new BigDecimal("1.00"));
            assertThat(p.getDescricao()).doesNotContain("\u0000");
        } catch (DomainException e) {
            // ok — descricao vazia/longa gerou DomainException
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
    void valor_menorQueMinimo_sempreLancaDomainException(
            @ForAll @BigRange(min = "-9999999", max = "0") BigDecimal valor) {

        assertThatThrownBy(() -> newService().criar("Ok", valor))
                .isInstanceOf(DomainException.class);
    }

    @Property
    void valor_valido_sempreCriaPedido(
            @ForAll @BigRange(min = "0.01", max = "9999999") BigDecimal valor) {

        Pedido p = newService().criar("Descrição válida", valor);
        assertThat(p.getValor()).isEqualByComparingTo(valor);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.PENDENTE);
    }

    @Property
    void descricao_comMaisDe255Chars_sempreLancaDomainException(
            @ForAll @StringLength(min = Pedido.MAX_DESCRICAO + 1, max = 1000)
            String descricao) {

        assertThatThrownBy(() -> newService().criar(descricao, new BigDecimal("1.00")))
                .isInstanceOf(DomainException.class);
    }

    @Property
    void pedido_idEDataCriacao_saoImutaveis(
            @ForAll @StringLength(min = 1, max = 50) String descricao) {

        PedidoRepository repo = Mockito.mock(PedidoRepository.class);
        PedidoMapper mapper = Mockito.mock(PedidoMapper.class);
        ProdutoServiceClient produtoServiceClient = Mockito.mock(ProdutoServiceClient.class);
        StatusHistoricoRepository statusRepo = Mockito.mock(StatusHistoricoRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        PedidoService service = new PedidoService(repo, mapper, produtoServiceClient, statusRepo);

        Pedido p = service.criar(descricao, new BigDecimal("5.00"));
        UUID idOriginal = p.getId();
        var dataOriginal = p.getDataCriacao();

        when(repo.findById(idOriginal)).thenReturn(Optional.of(p));
        service.atualizar(idOriginal, "Nova desc", new BigDecimal("9.99"), null);

        assertThat(p.getId()).isEqualTo(idOriginal);
        assertThat(p.getDataCriacao()).isEqualTo(dataOriginal);
    }
}
