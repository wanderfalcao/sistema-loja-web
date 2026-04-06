package br.com.infnet.produto.factory;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.Quantidade;
import br.com.infnet.produto.domain.Sku;
import br.com.infnet.produto.dto.ProdutoRequest;

import java.math.BigDecimal;

public class ProdutoFactory {

    private ProdutoFactory() {}

    public static Produto criar(String nome, BigDecimal preco, CategoriaProduto categoria) {
        Sku sku = Sku.gerar(nome);
        Produto p = Produto.novo(nome.trim(), sku, preco);
        p.definirCategoria(categoria);
        return p;
    }

    public static Produto criar(ProdutoRequest request) {
        Sku sku = Sku.gerar(request.getNome());
        Produto p = Produto.novo(request.getNome().trim(), sku, request.getPreco());
        if (request.getDescricao() != null) p.definirDescricao(request.getDescricao());
        p.definirEstoque(Quantidade.de(request.getEstoque() != null ? request.getEstoque() : 0));
        p.alterarAtivo(request.getAtivo() != null ? request.getAtivo() : true);
        if (request.getEstoqueMinimo() != null) p.definirEstoqueMinimo(Quantidade.de(request.getEstoqueMinimo()));
        p.definirCategoria(request.getCategoria());
        p.definirImagemUrl(request.getImagemUrl());
        return p;
    }

    public static void atualizar(Produto produto, ProdutoRequest request) {
        produto.atualizar(request.getNome(), produto.getSku(), request.getPreco());
        if (request.getDescricao() != null) produto.definirDescricao(request.getDescricao());
        if (request.getEstoque() != null) produto.definirEstoque(Quantidade.de(request.getEstoque()));
        produto.alterarAtivo(request.getAtivo());
        if (request.getEstoqueMinimo() != null) produto.definirEstoqueMinimo(Quantidade.de(request.getEstoqueMinimo()));
        produto.definirCategoria(request.getCategoria());
        produto.definirImagemUrl(request.getImagemUrl());
    }
}
