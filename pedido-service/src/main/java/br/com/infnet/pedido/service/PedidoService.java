package br.com.infnet.pedido.service;

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
import br.com.infnet.client.TipoOperacaoEstoque;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService implements CrudService<Pedido, UUID> {

    private static final String MSG_DESCRICAO_VAZIA    = "Descrição não pode ser vazia.";
    private static final String MSG_DESCRICAO_LONGA    = "Descrição deve ter no máximo " + Pedido.MAX_DESCRICAO + " caracteres.";
    private static final String MSG_VALOR_MINIMO       = "Valor deve ser no mínimo R$ 0,01.";
    private static final String MSG_TRANSICAO_INVALIDA = "Transição inválida: ";
    private static final String MSG_MOTIVO_OBRIGATORIO = "Motivo da contestação não pode ser vazio.";

    private final PedidoRepository repository;
    private final PedidoMapper mapper;
    private final ProdutoServiceClient produtoServiceClient;
    private final StatusHistoricoRepository statusHistoricoRepository;

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repository.findAllByOrderByDataCriacaoDesc();
    }

    @Transactional(readOnly = true)
    public Page<Pedido> listarPaginadoComFiltros(StatusPedido filtroStatus, String busca, Pageable pageable) {
        String buscaNorm = (busca != null && !busca.isBlank()) ? busca.trim() : null;
        StatusPedido statusNorm = filtroStatus;
        return repository.filtrar(statusNorm, buscaNorm, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional
    public PedidoResponse criarDTO(PedidoRequest request) {
        validarDescricao(request.getDescricao());
        validarValor(request.getValor());
        Pedido pedido = PedidoFactory.criar(request);
        return mapper.toResponse(repository.save(pedido));
    }

    @Transactional
    public PedidoResponse atualizarDTO(UUID id, PedidoRequest request) {
        validarDescricao(request.getDescricao());
        validarValor(request.getValor());
        Pedido pedido = buscar(id);
        pedido.setDescricao(request.getDescricao().trim());
        pedido.setValor(request.getValor());
        pedido.setObservacao(request.getObservacao() != null ? request.getObservacao().trim() : null);
        pedido.setDataAtualizacao(LocalDateTime.now());
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
        return statusHistoricoRepository.findAllByPedidoIdOrderByDataTransicaoAsc(pedidoId);
    }

    @Transactional
    public Pedido criar(String descricao, BigDecimal valor) {
        return criar(descricao, valor, null);
    }

    @Transactional
    public Pedido criar(String descricao, BigDecimal valor, String observacao) {
        validarDescricao(descricao);
        validarValor(valor);
        return repository.save(PedidoFactory.criar(descricao, valor, observacao));
    }

    @Transactional
    public Pedido atualizar(UUID id, String descricao, BigDecimal valor, String observacao) {
        validarDescricao(descricao);
        validarValor(valor);
        Pedido pedido = buscar(id);
        pedido.setDescricao(descricao.trim());
        pedido.setValor(valor);
        pedido.setObservacao(observacao != null ? observacao.trim() : null);
        pedido.setDataAtualizacao(LocalDateTime.now());
        return repository.save(pedido);
    }

    /**
     * Transições válidas:
     *   PENDENTE    → PROCESSANDO | CANCELADO
     *   PROCESSANDO → CONCLUIDO   | CANCELADO
     *   CONCLUIDO   → CONTESTADO
     *   CONTESTADO  → PROCESSANDO | CANCELADO
     *   CANCELADO   → (terminal)
     *   mesmo → mesmo (idempotente)
     */
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
        validarTransicao(atual, novoStatus);

        log.info("Pedido {} transicionando {} → {}", id, atual, novoStatus);

        // Baixa de estoque ao processar (chamada HTTP ao produto-service)
        if (atual == StatusPedido.PENDENTE && novoStatus == StatusPedido.PROCESSANDO) {
            debitarEstoque(pedido);
        }
        // Devolução de estoque ao cancelar (chamada HTTP ao produto-service)
        if (atual == StatusPedido.PROCESSANDO && novoStatus == StatusPedido.CANCELADO) {
            devolverEstoque(pedido);
        }

        pedido.setStatus(novoStatus);
        pedido.setDataAtualizacao(LocalDateTime.now());
        Pedido salvo = repository.save(pedido);

        statusHistoricoRepository.save(new StatusHistorico(salvo, atual, novoStatus, null));
        log.info("Histórico de status registrado: pedido={} {} → {}", id, atual, novoStatus);

        return salvo;
    }

    @Transactional
    public Pedido contestar(UUID id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new DomainException(MSG_MOTIVO_OBRIGATORIO);
        }
        Pedido pedido = buscar(id);
        StatusPedido atual = pedido.getStatus();
        validarTransicao(atual, StatusPedido.CONTESTADO);
        pedido.setStatus(StatusPedido.CONTESTADO);
        pedido.setObservacao(motivo.trim());
        pedido.setDataAtualizacao(LocalDateTime.now());
        Pedido salvo = repository.save(pedido);

        statusHistoricoRepository.save(new StatusHistorico(salvo, atual, StatusPedido.CONTESTADO, motivo.trim()));
        log.info("Pedido {} contestado. Motivo: {}", id, motivo.trim());

        return salvo;
    }

    @Transactional
    public PedidoResponse adicionarItem(UUID pedidoId, ItemPedidoRequest request) {
        Pedido pedido = buscar(pedidoId);
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new DomainException("Itens só podem ser adicionados a pedidos PENDENTE.");
        }
        // Auto-enriquecimento: busca dados oficiais do produto-service quando produtoId fornecido
        if (request.getProdutoId() != null) {
            ProdutoInfo info = produtoServiceClient.buscarProduto(request.getProdutoId());
            if (!info.isAtivo()) {
                throw new DomainException("Produto inativo não pode ser adicionado ao pedido.");
            }
            request.setNomeProduto(info.getNome());
            request.setSkuProduto(info.getSku());
            request.setPrecoUnitario(info.getPreco());
            log.info("Item enriquecido com dados do produto-service: id={}, nome={}", request.getProdutoId(), info.getNome());
        }
        if (request.getNomeProduto() == null || request.getNomeProduto().isBlank()) {
            throw new DomainException("Nome do produto é obrigatório.");
        }
        ItemPedido item = ItemPedidoFactory.criar(pedido, request);
        pedido.getItens().add(item);
        return mapper.toResponse(repository.save(pedido));
    }

    @Transactional
    public PedidoResponse removerItem(UUID pedidoId, UUID itemId) {
        Pedido pedido = buscar(pedidoId);
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new DomainException("Itens só podem ser removidos de pedidos PENDENTE.");
        }
        pedido.getItens().removeIf(i -> i.getId().equals(itemId));
        return mapper.toResponse(repository.save(pedido));
    }

    @Transactional
    public void deletar(UUID id) {
        Pedido pedido = buscar(id);
        repository.delete(pedido);
    }

    private void debitarEstoque(Pedido pedido) {
        pedido.getItens().stream()
              .filter(i -> i.getProdutoId() != null)
              .forEach(i -> produtoServiceClient.ajustarEstoque(
                  i.getProdutoId(), TipoOperacaoEstoque.SAIDA, i.getQuantidade()));
    }

    private void devolverEstoque(Pedido pedido) {
        pedido.getItens().stream()
              .filter(i -> i.getProdutoId() != null)
              .forEach(i -> produtoServiceClient.ajustarEstoque(
                  i.getProdutoId(), TipoOperacaoEstoque.ENTRADA, i.getQuantidade()));
    }

    private void validarTransicao(StatusPedido atual, StatusPedido novo) {
        boolean valida = switch (atual) {
            case PENDENTE    -> novo == StatusPedido.PROCESSANDO || novo == StatusPedido.CANCELADO;
            case PROCESSANDO -> novo == StatusPedido.CONCLUIDO   || novo == StatusPedido.CANCELADO;
            case CONCLUIDO   -> novo == StatusPedido.CONTESTADO;
            case CONTESTADO  -> novo == StatusPedido.PROCESSANDO || novo == StatusPedido.CANCELADO;
            case CANCELADO   -> false;
        };
        if (!valida) {
            throw new DomainException(MSG_TRANSICAO_INVALIDA + atual + " → " + novo);
        }
    }

    private void validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new DomainException(MSG_DESCRICAO_VAZIA);
        }
        if (descricao.trim().length() > Pedido.MAX_DESCRICAO) {
            throw new DomainException(MSG_DESCRICAO_LONGA);
        }
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(Pedido.VALOR_MINIMO) < 0) {
            throw new DomainException(MSG_VALOR_MINIMO);
        }
    }
}
