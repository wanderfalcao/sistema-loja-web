package br.com.infnet.produto.controller;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.exception.ProdutoNaoEncontradoException;
import br.com.infnet.produto.dto.ProdutoRequest;
import br.com.infnet.produto.service.ProdutoService;
import br.com.infnet.shared.exception.DomainException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) CategoriaProduto categoria,
                         @PageableDefault(size = 20, sort = "nome") Pageable pageable,
                         Model model) {
        Page<Produto> page = service.filtrar(busca, categoria, pageable);

        int pageStart = Math.max(0, page.getNumber() - 2);
        int pageEnd   = page.getTotalPages() > 0
                ? Math.min(page.getTotalPages() - 1, page.getNumber() + 2)
                : 0;

        model.addAttribute("produtos",           page.getContent());
        model.addAttribute("page",               page);
        model.addAttribute("busca",              busca);
        model.addAttribute("categoriaSelecionada", categoria);
        model.addAttribute("categorias",         CategoriaProduto.values());
        model.addAttribute("pageStart",          pageStart);
        model.addAttribute("pageEnd",            pageEnd);
        return "produtos/lista";
    }

    @GetMapping("/novo")
    public String formNovo(Model model) {
        model.addAttribute("titulo", "Novo Produto");
        model.addAttribute("acao", "/produtos");
        model.addAttribute("nome", "");
        model.addAttribute("preco", "");
        model.addAttribute("descricao", "");
        model.addAttribute("estoque", 0);
        model.addAttribute("ativo", true);
        model.addAttribute("categorias", CategoriaProduto.values());
        model.addAttribute("categoriaSelecionada", null);
        return "produtos/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("produto", service.buscarPorId(id));
            return "produtos/detalhe";
        } catch (ProdutoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/produtos";
        }
    }

    @GetMapping("/{id}/editar")
    public String formEditar(@PathVariable UUID id, Model model) {
        Produto produto = service.buscarPorId(id);
        model.addAttribute("titulo", "Editar Produto");
        model.addAttribute("acao", "/produtos/" + id);
        model.addAttribute("nome", produto.getNome());
        model.addAttribute("preco", produto.getPreco().quantia());
        model.addAttribute("descricao", produto.getDescricao());
        model.addAttribute("estoque", produto.getEstoque().inteiro());
        model.addAttribute("ativo", produto.getAtivo());
        model.addAttribute("categorias", CategoriaProduto.values());
        model.addAttribute("categoriaSelecionada", produto.getCategoria());
        model.addAttribute("imagemUrl", produto.getImagemUrl());
        return "produtos/form";
    }

    // ── Comandos ──────────────────────────────────────────────────────────────

    @PostMapping
    public String cadastrar(@RequestParam String nome,
                            @RequestParam BigDecimal preco,
                            @RequestParam(required = false) String descricao,
                            @RequestParam(defaultValue = "0") Integer estoque,
                            @RequestParam(defaultValue = "true") Boolean ativo,
                            @RequestParam(required = false) CategoriaProduto categoria,
                            @RequestParam(required = false) String imagemUrl,
                            Model model,
                            RedirectAttributes attrs) {
        try {
            ProdutoRequest request = ProdutoRequest.builder()
                    .nome(nome).preco(preco).descricao(descricao)
                    .estoque(estoque).ativo(ativo).categoria(categoria).imagemUrl(imagemUrl)
                    .build();
            service.criarDTO(request);
            attrs.addFlashAttribute("sucesso", "Produto cadastrado com sucesso.");
            return "redirect:/produtos";
        } catch (DomainException e) {
            model.addAttribute("titulo", "Novo Produto");
            model.addAttribute("acao", "/produtos");
            model.addAttribute("nome", nome);
            model.addAttribute("preco", preco);
            model.addAttribute("descricao", descricao);
            model.addAttribute("estoque", estoque);
            model.addAttribute("ativo", ativo);
            model.addAttribute("categorias", CategoriaProduto.values());
            model.addAttribute("categoriaSelecionada", categoria);
            model.addAttribute("imagemUrl", imagemUrl);
            model.addAttribute("erro", e.getMessage());
            return "produtos/form";
        }
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable UUID id,
                            @RequestParam String nome,
                            @RequestParam BigDecimal preco,
                            @RequestParam(required = false) String descricao,
                            @RequestParam(defaultValue = "0") Integer estoque,
                            @RequestParam(defaultValue = "true") Boolean ativo,
                            @RequestParam(required = false) CategoriaProduto categoria,
                            @RequestParam(required = false) String imagemUrl,
                            Model model,
                            RedirectAttributes attrs) {
        try {
            ProdutoRequest request = ProdutoRequest.builder()
                    .nome(nome).preco(preco).descricao(descricao)
                    .estoque(estoque).ativo(ativo).categoria(categoria).imagemUrl(imagemUrl)
                    .build();
            service.atualizarDTO(id, request);
            attrs.addFlashAttribute("sucesso", "Produto atualizado com sucesso.");
            return "redirect:/produtos";
        } catch (DomainException e) {
            model.addAttribute("titulo", "Editar Produto");
            model.addAttribute("acao", "/produtos/" + id);
            model.addAttribute("nome", nome);
            model.addAttribute("preco", preco);
            model.addAttribute("descricao", descricao);
            model.addAttribute("estoque", estoque);
            model.addAttribute("ativo", ativo);
            model.addAttribute("categorias", CategoriaProduto.values());
            model.addAttribute("categoriaSelecionada", categoria);
            model.addAttribute("imagemUrl", imagemUrl);
            model.addAttribute("erro", e.getMessage());
            return "produtos/form";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable UUID id, RedirectAttributes attrs) {
        service.remover(id);
        attrs.addFlashAttribute("sucesso", "Produto removido com sucesso.");
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/promocao")
    public String ativarPromocao(@PathVariable UUID id,
                                  @RequestParam BigDecimal percentual,
                                  @RequestParam(required = false) String dataInicio,
                                  @RequestParam(required = false) String dataFim,
                                  RedirectAttributes attrs) {
        try {
            service.ativarPromocao(id, percentual, parseDateTime(dataInicio), parseDateTime(dataFim));
            attrs.addFlashAttribute("sucesso", "Promoção ativada com sucesso.");
        } catch (DomainException e) {
            attrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/produtos/" + id;
    }

    @PostMapping("/{id}/promocao/encerrar")
    public String encerrarPromocao(@PathVariable UUID id, RedirectAttributes attrs) {
        try {
            service.encerrarPromocao(id);
            attrs.addFlashAttribute("sucesso", "Promoção encerrada com sucesso.");
        } catch (DomainException e) {
            attrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/produtos/" + id;
    }

    private static final DateTimeFormatter DT_FORM = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s, DT_FORM);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(s);
        }
    }
}
