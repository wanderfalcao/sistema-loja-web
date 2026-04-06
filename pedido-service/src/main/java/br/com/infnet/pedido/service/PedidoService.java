package br.com.infnet.pedido.service;

import br.com.infnet.pedido.domain.Dinheiro;
import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusHistorico;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.domain.exception.PedidoNaoEncontradoException;
import br.com.infnet.pedido.dto.ContestarRequest;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.dto.PedidoRequest;
import br.com.infnet.pedido.dto.PedidoResponse;
import br.com.infnet.pedido.factory.ItemPedidoFactory;
import br.com.infnet.pedido.factory.PedidoFactory;
import br.com.infnet.pedido.mapper.PedidoMapper;
import br.com.infnet.client.ProdutoInfo;
import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.pedido.repository.PedidoRepository;
import br.com.infnet.pedido.repository.StatusHistoricoRepository;
import br.com.infnet.shared.exception.DomainException;
import br.com.infnet.shared.service.CrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService implements CrudService<Pedido, UUID> {

    private final PedidoRepository repository;
    private final PedidoMapper mapper;
    private final ProdutoServiceClient produtoServiceClient;
    private final EstoqueOrquestrador estoqueOrquestrador;
    private final StatusHistoricoRegistrador historicoRegistrador;
    private final PedidoValidador validador;
    private final PedidoStatusMachine statusMachine;

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repository.findAllByOrderByDataCriacaoDesc();
    }

    @Transactional(readOnly = true)
    public Page<Pedido> listarPaginadoComFiltros(StatusPedido filtroStatus, String busca, Pageable pageable) {
        String buscaNorm = (busca != null && !busca.isBlank()) ? busca.trim().toLowerCase() : null;
        if (buscaNorm == null && filtroStatus == null) return repository.findAll(pageable);
        if (buscaNorm == null)                         return repository.findAllByStatus(filtroStatus, pageable);
        return repository.filtrarComDescricao(filtroStatus, buscaNorm, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional
    public PedidoResponse criarDTO(PedidoRequest request) {
        validador.validarDescricao(request.getDescricao());
        validador.validarValor(request.getValor());
        Pedido pedido = PedidoFactory.criar(request);
        return mapper.toResponse(repository.save(pedido));
    }

    @Transactional
    public PedidoResponse atualizarDTO(UUID id, PedidoRequest request) {
        validador.validarDescricao(request.getDescricao());
        validador.validarValor(request.getValor());
        Pedido pedido = buscar(id);
        pedido.atualizar(
            request.getDescricao().trim(),
            Dinheiro.de(request.getValor()),
            request.getObservacao() != null ? request.getObservacao().trim() : null
        );
        return mapper.toResponse(repository.save(pedido));
    }

    @Transactional
    public PedidoResponse contestarDTO(UUID id, ContestarRequest request) {
        return mapper.toResponse(contestar(id, request.getMotivo()));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarDTO(UUID id) {
        return mapper.toResponse(buscar(id));
    }

    @Transactional
    public PedidoResponse avancarStatusDTO(UUID id, StatusPedido novoStatus) {
        return mapper.toResponse(avancarStatus(id, novoStatus));
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido buscarPorId(UUID id) {
        return buscar(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return listar();
    }

    @Override
    @Transactional
    public void remover(UUID id) {
        deletar(id);
    }

    @Transactional(readOnly = true)
    public Pedido buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> contarPorStatus() {
        Map<String, Long> contagem = new LinkedHashMap<>();
        for (StatusPedido s : StatusPedido.values()) {
            contagem.put(s.name(), repository.countByStatus(s));
        }
        return contagem;
    }

    @Transactional(readOnly = true)
    public BigDecimal somarValoresAtivos() {
        BigDecimal soma = repository.somarValoresAtivos();
        return soma != null ? soma : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<StatusHistorico> buscarHistorico(UUID pedidoId) {
        return historicoRegistrador.buscarHistorico(pedidoId);
    }

    @Transactional
    public Pedido criar(String descricao, BigDecimal valor) {
        return criar(descricao, valor, null);
    }

    @Transactional
    public Pedido criar(String descricao, BigDecimal valor, String observacao) {
        validador.validarDescricao(descricao);
        validador.validarValor(valor);
        return repository.save(PedidoFactory.criar(descricao, valor, observacao));
    }

    @Transactional
    public Pedido atualizar(UUID id, String descricao, BigDecimal valor, String observacao) {
        validador.validarDescricao(descricao);
        validador.validarValor(valor);
        Pedido pedido = buscar(id);
        pedido.atualizar(
            descricao.trim(),
            Dinheiro.de(valor),
            observacao != null ? observacao.trim() : null
        );
        return repository.save(pedido);
    }

    @Transactional
    public Pedido avancarStatus(UUID id, StatusPedido novoStatus) {
        Pedido pedido = buscar(id);
        StatusPedido atual = pedido.getStatus();
        if (atual == novoStatus) {
            return pedido;
        }
        if (atual == StatusPedido.PENDENTE && novoStatus == StatusPedido.PROCESSANDO
                && pedido.getItens().isEmpty()) {
            throw new DomainException("Pedido deve ter pelo menos um item para ser processado.");
        }
        statusMachine.validarTransicao(atual, novoStatus);

        log.info("Pedido {} transicionando {} → {}", id, atual, novoStatus);

        statusMachine.resolverOperacaoEstoque(atual, novoStatus)
                     .ifPresent(op -> estoqueOrquestrador.aplicarOperacaoEstoque(pedido, op));

        pedido.avancarStatus(novoStatus);
        Pedido salvo = repository.save(pedido);

        historicoRegistrador.registrar(salvo, atual, novoStatus, null);

        return salvo;
    }

    @Transactional
    public Pedido contestar(UUID id, String motivo) {
        validador.validarMotivo(motivo);
        Pedido pedido = buscar(id);
        StatusPedido atual = pedido.getStatus();
        statusMachine.validarTransicao(atual, StatusPedido.CONTESTADO);

        // Verifica se há operação de estoque associada à transição de contestação
        statusMachine.resolverOperacaoEstoque(atual, StatusPedido.CONTESTADO)
                     .ifPresent(op -> estoqueOrquestrador.aplicarOperacaoEstoque(pedido, op));

        pedido.contestar(motivo.trim());
        Pedido salvo = repository.save(pedido);

        historicoRegistrador.registrar(salvo, atual, StatusPedido.CONTESTADO, motivo.trim());
        log.info("Pedido {} contestado. Motivo: {}", id, motivo.trim());

        return salvo;
    }

    @Transactional
    public PedidoResponse adicionarItem(UUID pedidoId, ItemPedidoRequest request) {
        Pedido pedido = buscar(pedidoId);
        if (pedido.getStatus() != StatusPedido.PENDENTE)
            throw new DomainException("Itens só podem ser adicionados a pedidos PENDENTE.");
        if (pedido.getItens().size() >= 20)
            throw new DomainException("Pedido não pode ter mais de 20 itens.");

        enriquecerComDadosDoProduto(request);

        if (request.getNomeProduto() == null || request.getNomeProduto().isBlank())
            throw new DomainException("Nome do produto é obrigatório.");

        ItemPedido item = ItemPedidoFactory.criar(pedido, request);
        pedido.getItens().add(item);
        return mapper.toResponse(repository.save(pedido));
    }

    @Transactional
    public PedidoResponse removerItem(UUID pedidoId, UUID itemId) {
        Pedido pedido = buscar(pedidoId);
        if (pedido.getStatus() != StatusPedido.PENDENTE)
            throw new DomainException("Itens só podem ser removidos de pedidos PENDENTE.");
        pedido.getItens().removeIf(i -> i.getId().equals(itemId));
        return mapper.toResponse(repository.save(pedido));
    }

    @Transactional
    public void deletar(UUID id) {
        Pedido pedido = buscar(id);
        repository.delete(pedido);
    }

    /** Enriquece o request com dados do produto-service quando um produtoId é informado. */
    private void enriquecerComDadosDoProduto(ItemPedidoRequest request) {
        if (request.getProdutoId() == null) return;
        ProdutoInfo info = produtoServiceClient.buscarProduto(request.getProdutoId());
        if (!info.isAtivo())
            throw new DomainException("Produto inativo não pode ser adicionado ao pedido.");
        request.setNomeProduto(info.getNome());
        request.setSkuProduto(info.getSku());
        request.setPrecoUnitario(info.getPreco());
        log.info("Item enriquecido com dados do produto-service: id={}, nome={}", request.getProdutoId(), info.getNome());
    }
}
