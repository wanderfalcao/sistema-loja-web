package br.com.infnet.produto.service;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.Quantidade;
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

@Service
@Transactional
@RequiredArgsConstructor
public class ProdutoService implements CrudService<Produto, UUID> {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;

    public Produto cadastrar(String nome, BigDecimal preco,
                              String descricao, Integer estoque,
                              CategoriaProduto categoria, String imagemUrl) {
        Produto p = ProdutoFactory.criar(nome, preco, categoria);
        if (descricao != null && !descricao.isBlank()) p.definirDescricao(descricao);
        if (estoque != null && estoque > 0)            p.definirEstoque(Quantidade.de(estoque));
        if (imagemUrl != null && !imagemUrl.isBlank()) p.definirImagemUrl(imagemUrl.trim());
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
        produto.definirDescricao(descricao);
        if (estoque != null) produto.definirEstoque(Quantidade.de(estoque));
        produto.alterarAtivo(ativo);
        produto.definirCategoria(categoria);
        produto.definirImagemUrl(imagemUrl != null && !imagemUrl.isBlank() ? imagemUrl.trim() : null);
        return repository.save(produto);
    }

    @Override
    public void remover(UUID id) {
        Produto produto = buscarPorId(id);
        if (produto.getEstoque().inteiro() > 0)
            throw new DomainException("Produto com estoque não pode ser removido. Zere o estoque antes de excluir.");
        repository.deleteById(id);
    }

    public ProdutoResponse criarDTO(ProdutoRequest request) {
        if (repository.existsByNomeIgnoreCase(request.getNome()))
            throw new DomainException("Produto com este nome já existe: " + request.getNome());

        Produto produto = ProdutoFactory.criar(request);

        if (repository.existsBySku(produto.getSku()))
            throw new DomainException("SKU já cadastrado: " + produto.getSku().codigo());

        validarAtivacaoComEstoque(produto);

        return mapper.toResponse(repository.save(produto));
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarDTO(UUID id) {
        return mapper.toResponse(buscarPorId(id));
    }

    public ProdutoResponse atualizarDTO(UUID id, ProdutoRequest request) {
        Produto produto = buscarPorId(id);

        if (repository.existsByNomeIgnoreCaseAndIdNot(request.getNome(), id))
            throw new DomainException("Produto com este nome já existe: " + request.getNome());

        ProdutoFactory.atualizar(produto, request);

        validarAtivacaoComEstoque(produto);

        return mapper.toResponse(repository.save(produto));
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(Pageable pageable) {
        return repository.findAllByAtivo(true, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<Produto> filtrar(String nome, CategoriaProduto categoria, Pageable pageable) {
        String nomeNorm = (nome != null && !nome.isBlank()) ? nome.trim().toLowerCase() : null;
        if (nomeNorm == null && categoria == null) return repository.findAll(pageable);
        if (nomeNorm == null)                      return repository.findAllByCategoria(categoria, pageable);
        return repository.filtrarComNome(nomeNorm, categoria, pageable);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorSku(String sku) {
        return repository.findBySku(br.com.infnet.produto.domain.Sku.de(sku))
                .map(mapper::toResponse)
                .orElseThrow(() -> new DomainException("Produto não encontrado para SKU: " + sku));
    }

    /** Delega o ajuste de estoque ao domínio, que usa TipoOperacaoEstoque polimorficamente. */
    public ProdutoResponse ajustarEstoque(UUID id, TipoOperacaoEstoque operacao, int quantidade) {
        if (quantidade <= 0)
            throw new DomainException("Quantidade deve ser maior que zero.");
        Produto produto = buscarPorId(id);
        produto.ajustarEstoque(operacao, Quantidade.de(quantidade));
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

    private void validarAtivacaoComEstoque(Produto produto) {
        if (Boolean.TRUE.equals(produto.getAtivo()) && produto.getEstoque().inteiro() == 0)
            throw new DomainException("Produto ativo deve ter estoque maior que zero.");
    }
}
