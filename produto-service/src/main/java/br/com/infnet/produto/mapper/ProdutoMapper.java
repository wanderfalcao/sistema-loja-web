package br.com.infnet.produto.mapper;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.dto.ProdutoRequest;
import br.com.infnet.produto.dto.ProdutoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    @Mapping(target = "categoriaNome",      expression = "java(produto.getCategoria() != null ? produto.getCategoria().getLabel() : null)")
    @Mapping(target = "precoComDesconto",   source = "promocao.precoComDesconto")
    @Mapping(target = "percentualDesconto", source = "promocao.percentualDesconto")
    @Mapping(target = "promocaoInicio",     source = "promocao.inicio")
    @Mapping(target = "promocaoFim",        source = "promocao.fim")
    ProdutoResponse toResponse(Produto produto);

    @Mapping(target = "categoriaNome",      expression = "java(produto.getCategoria() != null ? produto.getCategoria().getLabel() : null)")
    @Mapping(target = "precoComDesconto",   source = "promocao.precoComDesconto")
    @Mapping(target = "percentualDesconto", source = "promocao.percentualDesconto")
    @Mapping(target = "promocaoInicio",     source = "promocao.inicio")
    @Mapping(target = "promocaoFim",        source = "promocao.fim")
    List<ProdutoResponse> toResponseList(List<Produto> produtos);

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "dataCriacao",    ignore = true)
    @Mapping(target = "dataAtualizacao",ignore = true)
    @Mapping(target = "promocao",       ignore = true)
    Produto toEntity(ProdutoRequest request);

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "dataCriacao",    ignore = true)
    @Mapping(target = "dataAtualizacao",ignore = true)
    @Mapping(target = "promocao",       ignore = true)
    void updateEntity(ProdutoRequest request, @MappingTarget Produto produto);
}
