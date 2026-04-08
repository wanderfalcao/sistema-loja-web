package br.com.infnet.client;

import br.com.infnet.shared.exception.DomainException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProdutoServiceClientImpl implements ProdutoServiceClient {

    private static final String PRODUTOS_PATH = "/api/v1/produtos/";
    private static final String MSG_SERVICO_INDISPONIVEL = "Serviço de produtos temporariamente indisponível.";

    private final RestTemplate restTemplate;

    @Value("${produto.service.url:http://produto-service:8081}")
    private String produtoServiceUrl;

    @Value("${produto.service.page.size:200}")
    private int produtoPageSize;

    @Override
    public ProdutoInfo buscarProduto(UUID id) {
        String url = produtoServiceUrl + PRODUTOS_PATH + id;
        try {
            return restTemplate.getForObject(url, ProdutoInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new DomainException("Produto não encontrado: " + id);
        } catch (Exception e) {
            log.warn("Falha ao consultar produto-service para ID {}: {}", id, e.getMessage());
            throw new DomainException(MSG_SERVICO_INDISPONIVEL);
        }
    }

    @Override
    public List<ProdutoInfo> listarAtivos() {
        String url = produtoServiceUrl + "/api/v1/produtos?size=" + produtoPageSize + "&sort=nome";
        try {
            ProdutoPageResponse page = restTemplate.getForObject(url, ProdutoPageResponse.class);
            return page != null ? page.getContent() : List.of();
        } catch (Exception e) {
            log.warn("Falha ao listar produtos do produto-service (url={}): {}", url, e.getMessage());
            throw new DomainException("Não foi possível carregar os produtos. Tente novamente em instantes.");
        }
    }

    /**
     * Ajusta o estoque de um produto no produto-service.
     * Erros de rede (ResourceAccessException) e erros 5xx são retentados até 3 vezes
     * com backoff exponencial (500ms → 1s → 2s). Erros 4xx não são retentados
     * pois indicam problema de negócio (produto não encontrado, estoque insuficiente).
     */
    @Retryable(
        retryFor  = { ResourceAccessException.class, HttpServerErrorException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Override
    public void ajustarEstoque(UUID id, TipoOperacaoEstoque operacao, int quantidade) {
        String url = produtoServiceUrl + PRODUTOS_PATH + id + "/estoque";
        EstoqueAjusteRequest request = new EstoqueAjusteRequest(operacao, quantidade);
        try {
            restTemplate.patchForObject(url, request, Void.class);
        } catch (HttpClientErrorException e) {
            // 4xx: erro de negócio — não retentar
            log.warn("Erro ao ajustar estoque do produto {}: {}", id, e.getMessage());
            throw new DomainException("Erro ao ajustar estoque: " + e.getMessage());
        }
        // ResourceAccessException e HttpServerErrorException propagam para acionar retry
    }

    @Recover
    public void ajustarEstoqueRecovery(Exception ex, UUID id, TipoOperacaoEstoque operacao, int quantidade) {
        log.error("Falha ao ajustar estoque do produto {} após 3 tentativas — operacao={}, qtd={}",
                  id, operacao, quantidade, ex);
        throw new DomainException(MSG_SERVICO_INDISPONIVEL);
    }

    /** DTO interno para deserializar a resposta paginada de /api/v1/produtos. */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ProdutoPageResponse {
        private List<ProdutoInfo> content = List.of();
    }
}
