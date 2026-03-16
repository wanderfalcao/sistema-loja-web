package br.com.infnet.produto.controller;

import br.com.infnet.produto.dto.AjusteEstoqueRequest;
import br.com.infnet.produto.dto.ProdutoRequest;
import br.com.infnet.produto.dto.ProdutoResponse;
import br.com.infnet.produto.dto.PromocaoRequest;
import br.com.infnet.produto.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "API REST para gerenciamento de produtos")
public class ProdutoRestController {

    private final ProdutoService service;

    @GetMapping
    @Operation(summary = "Listar produtos ativos com paginação")
    public Page<ProdutoResponse> listar(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto por ID")
    public ProdutoResponse buscar(@PathVariable UUID id) {
        return service.buscarDTO(id);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Buscar produto por SKU")
    public ResponseEntity<ProdutoResponse> buscarPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(service.buscarPorSku(sku));
    }

    @PostMapping
    @Operation(summary = "Criar novo produto")
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        ProdutoResponse response = service.criarDTO(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto existente")
    public ProdutoResponse atualizar(@PathVariable UUID id,
                                     @Valid @RequestBody ProdutoRequest request) {
        return service.atualizarDTO(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover produto")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estoque")
    @Operation(summary = "Ajustar estoque (ENTRADA ou SAIDA)")
    public ProdutoResponse ajustarEstoque(@PathVariable UUID id,
                                          @Valid @RequestBody AjusteEstoqueRequest request) {
        return service.ajustarEstoque(id, request.getOperacao(), request.getQuantidade());
    }

    @PatchMapping("/{id}/promocao")
    @Operation(summary = "Ativar promoção em produto ativo")
    public ProdutoResponse ativarPromocao(@PathVariable UUID id,
                                          @Valid @RequestBody PromocaoRequest request) {
        return service.ativarPromocao(id, request.getPercentualDesconto(),
                                          request.getDataInicio(), request.getDataFim());
    }

    @DeleteMapping("/{id}/promocao")
    @Operation(summary = "Encerrar promoção do produto")
    public ProdutoResponse encerrarPromocao(@PathVariable UUID id) {
        return service.encerrarPromocao(id);
    }
}
