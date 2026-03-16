package br.com.infnet.client;

import br.com.infnet.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
    public void ajustarEstoque(UUID id, TipoOperacaoEstoque operacao, int quantidade) {
        String url = produtoServiceUrl + PRODUTOS_PATH + id + "/estoque";
        EstoqueAjusteRequest request = new EstoqueAjusteRequest(operacao, quantidade);
        try {
            restTemplate.patchForObject(url, request, Void.class);
        } catch (HttpClientErrorException e) {
            log.warn("Erro ao ajustar estoque do produto {}: {}", id, e.getMessage());
            throw new DomainException("Erro ao ajustar estoque: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Falha ao contactar produto-service para ajuste de estoque: {}", e.getMessage());
            throw new DomainException(MSG_SERVICO_INDISPONIVEL);
        }
    }
}
