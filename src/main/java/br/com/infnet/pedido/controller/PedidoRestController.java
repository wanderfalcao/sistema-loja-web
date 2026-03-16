package br.com.infnet.pedido.controller;

import br.com.infnet.pedido.domain.StatusPedido;
import br.com.infnet.pedido.dto.ContestarRequest;
import br.com.infnet.pedido.dto.ItemPedidoRequest;
import br.com.infnet.pedido.dto.ItemPedidoResponse;
import br.com.infnet.pedido.dto.PedidoRequest;
import br.com.infnet.pedido.dto.PedidoResponse;
import br.com.infnet.pedido.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Listar pedidos paginados")
    public ResponseEntity<Page<PedidoResponse>> listar(
            @PageableDefault(size = 10, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido por ID")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarDTO(id));
    }

    @PostMapping
    @Operation(summary = "Criar novo pedido")
    public ResponseEntity<PedidoResponse> criar(@RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarDTO(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar pedido existente")
    public ResponseEntity<PedidoResponse> atualizar(@PathVariable UUID id,
                                                     @RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.ok(service.atualizarDTO(id, request));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Avançar status do pedido")
    public ResponseEntity<PedidoResponse> avancarStatus(@PathVariable UUID id,
                                                         @RequestParam StatusPedido novoStatus) {
        return ResponseEntity.ok(service.avancarStatusDTO(id, novoStatus));
    }

    @PostMapping("/{id}/contestar")
    @Operation(summary = "Contestar pedido concluído")
    public ResponseEntity<PedidoResponse> contestar(@PathVariable UUID id,
                                                     @RequestBody @Valid ContestarRequest request) {
        return ResponseEntity.ok(service.contestarDTO(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar pedido")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/itens")
    @Operation(summary = "Adicionar item ao pedido (apenas PENDENTE)")
    public ResponseEntity<PedidoResponse> adicionarItem(@PathVariable UUID id,
                                                         @RequestBody @Valid ItemPedidoRequest request) {
        return ResponseEntity.ok(service.adicionarItem(id, request));
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @Operation(summary = "Remover item do pedido (apenas PENDENTE)")
    public ResponseEntity<PedidoResponse> removerItem(@PathVariable UUID id,
                                                       @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.removerItem(id, itemId));
    }

    @GetMapping("/{id}/itens")
    @Operation(summary = "Listar itens do pedido")
    public ResponseEntity<List<ItemPedidoResponse>> listarItens(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarDTO(id).getItens());
    }
}
