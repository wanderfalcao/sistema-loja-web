package br.com.infnet.produto.repository;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    List<Produto> findAllByAtivoTrueOrderByNomeAsc();

    Page<Produto> findAllByAtivo(Boolean ativo, Pageable pageable);

    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    boolean existsBySku(br.com.infnet.produto.domain.Sku sku);
    boolean existsBySkuAndIdNot(br.com.infnet.produto.domain.Sku sku, UUID id);

    java.util.Optional<Produto> findBySku(br.com.infnet.produto.domain.Sku sku);

    List<Produto> findAllByCategoria(CategoriaProduto categoria);

    boolean existsByCategoriaAndAtivoTrue(CategoriaProduto categoria);

    Page<Produto> findAllByCategoria(CategoriaProduto categoria, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Produto p WHERE " +
            "(:categoria IS NULL OR p.categoria = :categoria) AND " +
            "LOWER(p.nome) LIKE CONCAT('%', :nome, '%')")
    Page<Produto> filtrarComNome(@org.springframework.data.repository.query.Param("nome") String nome,
                                  @org.springframework.data.repository.query.Param("categoria") CategoriaProduto categoria,
                                  Pageable pageable);
}
