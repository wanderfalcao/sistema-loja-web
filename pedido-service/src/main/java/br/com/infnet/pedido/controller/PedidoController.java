package br.com.infnet.pedido.controller;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.shared.PaginacaoConstants;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.service.PedidoService;
import br.com.infnet.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private static final String REDIRECT_LISTA       = "redirect:/pedidos";
    private static final String VIEW_LISTA           = "pedidos/list";
    private static final String VIEW_FORM            = "pedidos/form";
    private static final String VIEW_DETALHE         = "pedidos/detail";
    private static final String FLASH_SUCESSO        = "sucesso";
    private static final String FLASH_ERRO           = "erro";

    private static final String MSG_CRIADO           = "Pedido criado com sucesso!";
    private static final String MSG_ATUALIZADO       = "Pedido atualizado com sucesso!";
    private static final String MSG_REMOVIDO         = "Pedido removido.";
    private static final String MSG_STATUS_FMT       = "Status atualizado para %s.";
    private static final String MSG_CONTESTADO       = "Pedido contestado.";
    private static final String MSG_VALOR_VAZIO      = "Valor não pode ser vazio.";
    private static final String MSG_VALOR_INVALIDO   = "Valor inválido: \"%s\". Use formato numérico (ex: 10.50).";
    private static final String MSG_ITEM_ADICIONADO  = "Item adicionado ao pedido.";
    private static final String MSG_ITEM_REMOVIDO    = "Item removido do pedido.";

    private final PedidoService service;

    @GetMapping
    public String listar(
            @PageableDefault(size = PaginacaoConstants.PAGE_SIZE, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) StatusPedido filtroStatus,
            @RequestParam(required = false) String busca,
            Model model) {

        Page<Pedido> page = service.listarPaginadoComFiltros(filtroStatus, busca, pageable);

        Sort.Order order = pageable.getSort().isSorted()
                ? pageable.getSort().iterator().next()
                : Sort.Order.desc("dataCriacao");
        String sortField = order.getProperty();
        String sortDir   = order.getDirection().name().toLowerCase();

        int pageStart = Math.max(0, page.getNumber() - PaginacaoConstants.PAGINATION_WINDOW);
        int pageEnd   = page.getTotalPages() > 0
                ? Math.min(page.getTotalPages() - 1, page.getNumber() + PaginacaoConstants.PAGINATION_WINDOW)
                : 0;

        model.addAttribute("pedidos",           page.getContent());
        model.addAttribute("page",              page);
        model.addAttribute("sortField",         sortField);
        model.addAttribute("sortDir",           sortDir);
        model.addAttribute("pageStart",         pageStart);
        model.addAttribute("pageEnd",           pageEnd);
        model.addAttribute("status",            StatusPedido.values());
        model.addAttribute("filtroStatus",      filtroStatus);
        model.addAttribute("busca",             busca);
        model.addAttribute("contagemPorStatus", service.contarPorStatus());
        model.addAttribute("totalAtivo",        service.somarValoresAtivos());
        return VIEW_LISTA;
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable UUID id, Model model) {
        model.addAttribute("pedido",    service.buscar(id));
        model.addAttribute("status",   StatusPedido.values());
        model.addAttribute("historico", service.buscarHistorico(id));
        return VIEW_DETALHE;
    }

    @GetMapping("/novo")
    public String formularioNovo(Model model) {
        model.addAttribute("status", StatusPedido.values());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@RequestParam String descricao,
                        @RequestParam String valor,
                        @RequestParam(required = false) String observacao,
                        RedirectAttributes ra) {
        service.criar(descricao, parsearValor(valor), observacao);
        ra.addFlashAttribute(FLASH_SUCESSO, MSG_CRIADO);
        return REDIRECT_LISTA;
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable UUID id, Model model) {
        model.addAttribute("pedido",  service.buscar(id));
        model.addAttribute("status", StatusPedido.values());
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable UUID id,
                            @RequestParam String descricao,
                            @RequestParam String valor,
                            @RequestParam(required = false) String observacao,
                            RedirectAttributes ra) {
        service.atualizar(id, descricao, parsearValor(valor), observacao);
        ra.addFlashAttribute(FLASH_SUCESSO, MSG_ATUALIZADO);
        return REDIRECT_LISTA;
    }

    @PostMapping("/{id}/status")
    public String avancarStatus(@PathVariable UUID id,
                                @RequestParam StatusPedido novoStatus,
                                RedirectAttributes ra) {
        service.avancarStatus(id, novoStatus);
        ra.addFlashAttribute(FLASH_SUCESSO, String.format(MSG_STATUS_FMT, novoStatus));
        return REDIRECT_LISTA;
    }

    @PostMapping("/{id}/contestar")
    public String contestar(@PathVariable UUID id,
                            @RequestParam(required = false) String motivo,
                            RedirectAttributes ra) {
        service.contestar(id, motivo);
        ra.addFlashAttribute(FLASH_SUCESSO, MSG_CONTESTADO);
        return REDIRECT_LISTA;
    }

    @PostMapping("/{id}/deletar")
    public String deletar(@PathVariable UUID id, RedirectAttributes ra) {
        service.deletar(id);
        ra.addFlashAttribute(FLASH_SUCESSO, MSG_REMOVIDO);
        return REDIRECT_LISTA;
    }

    @PostMapping("/{id}/itens")
    public String adicionarItem(@PathVariable UUID id,
                                @RequestParam(required = false) String nomeProduto,
                                @RequestParam(required = false) String precoUnitario,
                                @RequestParam Integer quantidade,
                                @RequestParam(required = false) String skuProduto,
                                @RequestParam(required = false) UUID produtoId,
                                RedirectAttributes ra) {
        BigDecimal preco = (precoUnitario != null && !precoUnitario.isBlank())
                           ? parsearValor(precoUnitario) : null;
        ItemPedidoRequest request = new ItemPedidoRequest(
                produtoId, nomeProduto, skuProduto, preco, quantidade);
        service.adicionarItem(id, request);
        ra.addFlashAttribute(FLASH_SUCESSO, MSG_ITEM_ADICIONADO);
        return redirectDetalhe(id);
    }

    @PostMapping("/{id}/itens/{itemId}/remover")
    public String removerItem(@PathVariable UUID id,
                              @PathVariable UUID itemId,
                              RedirectAttributes ra) {
        service.removerItem(id, itemId);
        ra.addFlashAttribute(FLASH_SUCESSO, MSG_ITEM_REMOVIDO);
        return redirectDetalhe(id);
    }

    private static String redirectDetalhe(UUID id) {
        return "redirect:/pedidos/" + id;
    }

    /**
     * Fail early: converte string de valor para BigDecimal aceitando vírgula (pt-BR).
     * Lança DomainException para que o GlobalExceptionHandler redirecione
     * com mensagem amigável, sem expor NumberFormatException ao usuário.
     */
    private BigDecimal parsearValor(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new DomainException(MSG_VALOR_VAZIO);
        }
        try {
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new DomainException(String.format(MSG_VALOR_INVALIDO, valor));
        }
    }
}
