package br.com.infnet.produto.mapper;

import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.dto.ProdutoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    @Mapping(target = "preco",              expression = "java(produto.getPreco().quantia())")
    @Mapping(target = "sku",               expression = "java(produto.getSku().codigo())")
    @Mapping(target = "estoque",           expression = "java(produto.getEstoque().inteiro())")
    @Mapping(target = "estoqueMinimo",     expression = "java(produto.getEstoqueMinimo().inteiro())")
    @Mapping(target = "categoriaNome",     expression = "java(produto.getCategoria() != null ? produto.getCategoria().getLabel() : null)")
    @Mapping(target = "precoComDesconto",  source = "promocao.precoComDesconto")
    @Mapping(target = "percentualDesconto",source = "promocao.percentualDesconto")
    @Mapping(target = "promocaoInicio",    source = "promocao.inicio")
    @Mapping(target = "promocaoFim",       source = "promocao.fim")
    ProdutoResponse toResponse(Produto produto);

    List<ProdutoResponse> toResponseList(List<Produto> produtos);
}
