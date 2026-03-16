package br.com.infnet.produto.controller;

import br.com.infnet.produto.dto.AjusteEstoqueRequest;
import br.com.infnet.produto.dto.ProdutoRequest;
import br.com.infnet.produto.dto.ProdutoResponse;
import br.com.infnet.produto.dto.PromocaoRequest;
import br.com.infnet.produto.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Listar produtos ativos com paginação",
               description = "Retorna uma página de produtos ativos ordenados por nome. Suporta paginação padrão Spring Data (page, size, sort).")
    public Page<ProdutoResponse> listar(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto por ID",
               description = "Retorna os dados completos de um produto pelo seu UUID. Inclui preço promocional se houver promoção ativa.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto encontrado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ProdutoResponse buscar(@PathVariable UUID id) {
        return service.buscarDTO(id);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Buscar produto por SKU",
               description = "Retorna os dados completos de um produto pelo seu SKU (insensível a maiúsculas/minúsculas).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto encontrado"),
        @ApiResponse(responseCode = "422", description = "SKU não encontrado")
    })
    public ResponseEntity<ProdutoResponse> buscarPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(service.buscarPorSku(sku));
    }

    @PostMapping
    @Operation(summary = "Criar novo produto",
               description = "Cria um novo produto. Nome e SKU devem ser únicos. SKU é gerado automaticamente a partir do nome.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (validação de campos)"),
        @ApiResponse(responseCode = "422", description = "Regra de negócio violada (nome/SKU duplicado)")
    })
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        ProdutoResponse response = service.criarDTO(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto existente",
               description = "Atualiza os dados de um produto existente. Nome deve permanecer único.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    })
    public ProdutoResponse atualizar(@PathVariable UUID id,
                                     @Valid @RequestBody ProdutoRequest request) {
        return service.atualizarDTO(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover produto",
               description = "Remove permanentemente um produto. Não é possível remover produto com estoque maior que zero.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Produto removido"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "422", description = "Produto com estoque não pode ser removido")
    })
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estoque")
    @Operation(summary = "Ajustar estoque (ENTRADA ou SAIDA)",
               description = "Incrementa (ENTRADA) ou decrementa (SAIDA) o estoque do produto. " +
                             "SAIDA exige produto ativo e estoque suficiente. " +
                             "Se após SAIDA o estoque ficar igual ou abaixo do estoqueMinimo, o produto é desativado automaticamente. " +
                             "Retorna 422 se regra de negócio for violada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estoque ajustado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (operação ou quantidade ausente/inválida)"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "422", description = "Regra de negócio violada (produto inativo, estoque insuficiente)")
    })
    public ProdutoResponse ajustarEstoque(@PathVariable UUID id,
                                          @Valid @RequestBody AjusteEstoqueRequest request) {
        return service.ajustarEstoque(id, request.getOperacao(), request.getQuantidade());
    }

    @PatchMapping("/{id}/promocao")
    @Operation(summary = "Ativar promoção em produto ativo",
               description = "Cria uma promoção com percentual de desconto e datas de início/fim. " +
                             "Apenas produtos ativos podem ter promoção. " +
                             "O preço com desconto é calculado automaticamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Promoção ativada"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
        @ApiResponse(responseCode = "422", description = "Produto inativo ou datas inválidas")
    })
    public ProdutoResponse ativarPromocao(@PathVariable UUID id,
                                          @Valid @RequestBody PromocaoRequest request) {
        return service.ativarPromocao(id, request.getPercentualDesconto(),
                                          request.getDataInicio(), request.getDataFim());
    }

    @DeleteMapping("/{id}/promocao")
    @Operation(summary = "Encerrar promoção do produto",
               description = "Remove a promoção ativa do produto. Operação idempotente — se não há promoção, não falha.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Promoção encerrada"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ProdutoResponse encerrarPromocao(@PathVariable UUID id) {
        return service.encerrarPromocao(id);
    }
}
