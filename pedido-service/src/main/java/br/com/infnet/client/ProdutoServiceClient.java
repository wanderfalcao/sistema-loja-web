package br.com.infnet.client;

import java.util.UUID;

public interface ProdutoServiceClient {
    ProdutoInfo buscarProduto(UUID id);
    void ajustarEstoque(UUID id, TipoOperacaoEstoque operacao, int quantidade);
}
