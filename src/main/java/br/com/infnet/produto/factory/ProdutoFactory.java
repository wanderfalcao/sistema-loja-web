package br.com.infnet.produto.factory;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.SkuGenerator;
import br.com.infnet.produto.dto.ProdutoRequest;

import java.math.BigDecimal;

public class ProdutoFactory {

    private ProdutoFactory() {}

    /** Criação via MVC: SKU gerado automaticamente a partir do nome. */
    public static Produto criar(String nome, BigDecimal preco, CategoriaProduto categoria) {
        String sku = SkuGenerator.fromNome(nome);
        Produto p = Produto.novo(nome.trim(), sku, preco);
        p.setCategoria(categoria);
        return p;
    }

    /** Criação via REST: categoria vem do request. */
    public static Produto criar(ProdutoRequest request) {
        String sku = SkuGenerator.fromNome(request.getNome());
        Produto p = Produto.novo(request.getNome().trim(), sku, request.getPreco());
        if (request.getDescricao() != null) p.setDescricao(request.getDescricao());
        p.setEstoque(request.getEstoque() != null ? request.getEstoque() : 0);
        p.setAtivo(request.getAtivo() != null ? request.getAtivo() : true);
        if (request.getEstoqueMinimo() != null) p.setEstoqueMinimo(request.getEstoqueMinimo());
        p.setCategoria(request.getCategoria());
        p.setImagemUrl(request.getImagemUrl());
        return p;
    }

    /** Atualização via REST: preserva SKU existente (imutável após criação). */
    public static void atualizar(Produto produto, ProdutoRequest request) {
        produto.atualizar(request.getNome(), produto.getSku(), request.getPreco());
        if (request.getDescricao() != null) produto.setDescricao(request.getDescricao());
        if (request.getEstoque() != null) produto.setEstoque(request.getEstoque());
        if (request.getAtivo() != null) produto.setAtivo(request.getAtivo());
        if (request.getEstoqueMinimo() != null) produto.setEstoqueMinimo(request.getEstoqueMinimo());
        produto.setCategoria(request.getCategoria());
        produto.setImagemUrl(request.getImagemUrl());
    }
}
