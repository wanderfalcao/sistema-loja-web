package br.com.infnet.produto.repository;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Porta de persistência do domínio Produto.
 *
 * <p>Estende {@link JpaRepository} para herdar as operações CRUD padrão.
 * Métodos de query derivados são gerados pelo Spring Data JPA em tempo de execução.
 */
public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    // Listagem pública: só produtos ativos, ordenados por nome
    List<Produto> findAllByAtivoTrueOrderByNomeAsc();

    // Paginação com filtro de ativo
    Page<Produto> findAllByAtivo(Boolean ativo, Pageable pageable);

    // Validação de unicidade de nome
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    // Validação de unicidade de SKU
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    // Busca por SKU
    java.util.Optional<Produto> findBySkuIgnoreCase(String sku);

    // Busca por categoria
    List<Produto> findAllByCategoria(CategoriaProduto categoria);

    // Verifica se há produtos ativos em uma categoria
    boolean existsByCategoriaAndAtivoTrue(CategoriaProduto categoria);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Produto p WHERE " +
            "(:nome IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%',:nome,'%'))) AND " +
            "(:categoria IS NULL OR p.categoria = :categoria)")
    Page<Produto> filtrar(@org.springframework.data.repository.query.Param("nome") String nome,
                          @org.springframework.data.repository.query.Param("categoria") CategoriaProduto categoria,
                          Pageable pageable);
}
