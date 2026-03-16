package br.com.infnet.produto.domain;

public enum TipoOperacaoEstoque {
    ENTRADA,  // aumenta o estoque (reposição, devolução)
    SAIDA     // diminui o estoque (venda, pedido confirmado)
}
