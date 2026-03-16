package br.com.infnet.pedido.controller;

import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.dto.ContestarRequest;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.dto.ItemPedidoResponse;
import br.com.infnet.pedido.dto.PedidoRequest;
import br.com.infnet.pedido.dto.PedidoResponse;
import br.com.infnet.pedido.service.PedidoService;
import br.com.infnet.shared.PaginacaoConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "API REST para gerenciamento de pedidos")
public class PedidoRestController {

    private final PedidoService service;

    @GetMapping
    @Operation(summary = "Listar pedidos paginados",
               description = "Retorna uma página de pedidos ordenados por dataCriacao DESC. Suporta paginação padrão Spring Data.")
    public ResponseEntity<Page<PedidoResponse>> listar(
            @PageableDefault(size = PaginacaoConstants.PAGE_SIZE, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido por ID",
               description = "Retorna os dados completos do pedido, incluindo itens e valorTotal calculado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<PedidoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarDTO(id));
    }

    @PostMapping
    @Operation(summary = "Criar novo pedido",
               description = "Cria um novo pedido com status PENDENTE. O pedido deve ter descrição e valor válidos.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (validação de campos)"),
        @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    })
    public ResponseEntity<PedidoResponse> criar(@RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarDTO(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar pedido existente",
               description = "Atualiza descrição, valor e observação de um pedido existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido atualizado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    })
    public ResponseEntity<PedidoResponse> atualizar(@PathVariable UUID id,
                                                     @RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.ok(service.atualizarDTO(id, request));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Avançar status do pedido",
               description = "Transições válidas: PENDENTE→PROCESSANDO|CANCELADO, " +
                             "PROCESSANDO→CONCLUIDO|CANCELADO, CONCLUIDO→CONTESTADO, " +
                             "CONTESTADO→PROCESSANDO|CANCELADO. " +
                             "PENDENTE→PROCESSANDO requer pelo menos um item e debita estoque no produto-service. " +
                             "PROCESSANDO→CANCELADO devolve estoque ao produto-service. " +
                             "Transição para o mesmo status é idempotente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Transição de status inválida ou regra de negócio violada"),
        @ApiResponse(responseCode = "502", description = "Erro de comunicação com produto-service")
    })
    public ResponseEntity<PedidoResponse> avancarStatus(@PathVariable UUID id,
                                                         @RequestParam StatusPedido novoStatus) {
        return ResponseEntity.ok(service.avancarStatusDTO(id, novoStatus));
    }

    @PostMapping("/{id}/contestar")
    @Operation(summary = "Contestar pedido concluído",
               description = "Transiciona pedido de CONCLUIDO para CONTESTADO informando motivo. " +
                             "Apenas pedidos CONCLUIDO podem ser contestados. " +
                             "O motivo fica registrado como observação do pedido.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido contestado"),
        @ApiResponse(responseCode = "400", description = "Motivo ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Pedido não está em status CONCLUIDO")
    })
    public ResponseEntity<PedidoResponse> contestar(@PathVariable UUID id,
                                                     @RequestBody @Valid ContestarRequest request) {
        return ResponseEntity.ok(service.contestarDTO(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar pedido",
               description = "Remove permanentemente o pedido e todos os seus itens.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pedido removido"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/itens")
    @Operation(summary = "Adicionar item ao pedido (apenas PENDENTE)",
               description = "Adiciona um item ao pedido. Só é permitido para pedidos no status PENDENTE. " +
                             "Se produtoId for fornecido, nome/SKU/preço são automaticamente preenchidos " +
                             "com dados do produto-service (e o produto deve estar ativo).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item adicionado"),
        @ApiResponse(responseCode = "400", description = "Dados do item inválidos"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Pedido não está PENDENTE ou produto inativo"),
        @ApiResponse(responseCode = "502", description = "Erro de comunicação com produto-service")
    })
    public ResponseEntity<PedidoResponse> adicionarItem(@PathVariable UUID id,
                                                         @RequestBody @Valid ItemPedidoRequest request) {
        return ResponseEntity.ok(service.adicionarItem(id, request));
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @Operation(summary = "Remover item do pedido (apenas PENDENTE)",
               description = "Remove um item do pedido. Só é permitido para pedidos no status PENDENTE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item removido"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Pedido não está PENDENTE")
    })
    public ResponseEntity<PedidoResponse> removerItem(@PathVariable UUID id,
                                                       @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.removerItem(id, itemId));
    }

    @GetMapping("/{id}/itens")
    @Operation(summary = "Listar itens do pedido",
               description = "Retorna todos os itens de um pedido com snapshot de produto (nome, SKU, preço unitário, subtotal).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de itens"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<List<ItemPedidoResponse>> listarItens(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarDTO(id).getItens());
    }
}
