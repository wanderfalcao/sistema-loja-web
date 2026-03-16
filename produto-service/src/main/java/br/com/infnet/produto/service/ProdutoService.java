package br.com.infnet.produto.service;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.TipoOperacaoEstoque;
import br.com.infnet.produto.domain.exception.ProdutoNaoEncontradoException;
import br.com.infnet.produto.dto.ProdutoRequest;
import br.com.infnet.produto.dto.ProdutoResponse;
import br.com.infnet.produto.factory.ProdutoFactory;
import br.com.infnet.produto.mapper.ProdutoMapper;
import br.com.infnet.produto.repository.ProdutoRepository;
import br.com.infnet.shared.exception.DomainException;
import br.com.infnet.shared.service.CrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra as operações de negócio sobre Produto.
 *
 * <p>SKU é sempre gerado automaticamente por {@link br.com.infnet.produto.domain.SkuGenerator}.
 * Métodos com sufixo DTO servem a camada REST; os demais servem o MVC Thymeleaf.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProdutoService implements CrudService<Produto, UUID> {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;

    // ── MVC ───────────────────────────────────────────────────────────────────

    public Produto cadastrar(String nome, BigDecimal preco,
                              String descricao, Integer estoque,
                              CategoriaProduto categoria, String imagemUrl) {
        Produto p = ProdutoFactory.criar(nome, preco, categoria);
        if (descricao != null && !descricao.isBlank()) p.setDescricao(descricao);
        if (estoque != null && estoque > 0)            p.setEstoque(estoque);
        if (imagemUrl != null && !imagemUrl.isBlank()) p.setImagemUrl(imagemUrl.trim());
        return repository.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Produto buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> listarTodos() {
        return repository.findAll();
    }

    public Produto atualizar(UUID id, String nome, BigDecimal preco,
                              String descricao, Integer estoque, Boolean ativo,
                              CategoriaProduto categoria, String imagemUrl) {
        Produto produto = buscarPorId(id);
        produto.atualizar(nome, produto.getSku(), preco);
        produto.setDescricao(descricao);
        if (estoque != null) produto.setEstoque(estoque);
        if (ativo != null)   produto.setAtivo(ativo);
        produto.setCategoria(categoria);
        produto.setImagemUrl(imagemUrl != null && !imagemUrl.isBlank() ? imagemUrl.trim() : null);
        return repository.save(produto);
    }

    @Override
    public void remover(UUID id) {
        Produto produto = buscarPorId(id);
        if (produto.getEstoque() > 0)
            throw new DomainException("Produto com estoque nao pode ser removido. Zere o estoque antes de excluir.");
        repository.deleteById(id);
    }

    // ── REST / DTO ────────────────────────────────────────────────────────────

    public ProdutoResponse criarDTO(ProdutoRequest request) {
        if (repository.existsByNomeIgnoreCase(request.getNome()))
            throw new DomainException("Produto com este nome ja existe: " + request.getNome());

        Produto produto = ProdutoFactory.criar(request);

        if (repository.existsBySkuIgnoreCase(produto.getSku()))
            throw new DomainException("SKU ja cadastrado: " + produto.getSku());

        if (Boolean.TRUE.equals(produto.getAtivo()) && produto.getEstoque() == 0)
            throw new DomainException("Produto ativo deve ter estoque maior que zero.");

        return mapper.toResponse(repository.save(produto));
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarDTO(UUID id) {
        return mapper.toResponse(buscarPorId(id));
    }

    public ProdutoResponse atualizarDTO(UUID id, ProdutoRequest request) {
        Produto produto = buscarPorId(id);

        if (repository.existsByNomeIgnoreCaseAndIdNot(request.getNome(), id))
            throw new DomainException("Produto com este nome ja existe: " + request.getNome());

        ProdutoFactory.atualizar(produto, request);

        if (Boolean.TRUE.equals(produto.getAtivo()) && produto.getEstoque() == 0)
            throw new DomainException("Produto ativo deve ter estoque maior que zero.");

        return mapper.toResponse(repository.save(produto));
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(Pageable pageable) {
        return repository.findAllByAtivo(true, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorSku(String sku) {
        return repository.findBySkuIgnoreCase(sku)
                .map(mapper::toResponse)
                .orElseThrow(() -> new DomainException("Produto não encontrado para SKU: " + sku));
    }

    public ProdutoResponse ajustarEstoque(UUID id, TipoOperacaoEstoque operacao, int quantidade) {
        if (quantidade <= 0)
            throw new DomainException("Quantidade deve ser maior que zero.");

        Produto produto = buscarPorId(id);

        if (operacao == TipoOperacaoEstoque.SAIDA) {
            if (!Boolean.TRUE.equals(produto.getAtivo()))
                throw new DomainException("Saida de estoque nao permitida para produto inativo.");
            if (produto.getEstoque() < quantidade)
                throw new DomainException("Estoque insuficiente. Disponivel: " + produto.getEstoque());
            produto.setEstoque(produto.getEstoque() - quantidade);
            if (produto.getEstoque() <= produto.getEstoqueMinimo())
                produto.desativar();
        } else {
            produto.setEstoque(produto.getEstoque() + quantidade);
            if (Boolean.FALSE.equals(produto.getAtivo()))
                produto.ativar();
        }

        return mapper.toResponse(repository.save(produto));
    }

    public ProdutoResponse ativarPromocao(UUID id, BigDecimal percentual,
                                           LocalDateTime inicio, LocalDateTime fim) {
        Produto produto = buscarPorId(id);
        produto.ativarPromocao(percentual, inicio, fim);
        return mapper.toResponse(repository.save(produto));
    }

    public ProdutoResponse encerrarPromocao(UUID id) {
        Produto produto = buscarPorId(id);
        produto.encerrarPromocao();
        return mapper.toResponse(repository.save(produto));
    }
}
