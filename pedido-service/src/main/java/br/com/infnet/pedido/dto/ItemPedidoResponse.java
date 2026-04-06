package br.com.infnet.pedido.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode
@ToString
@Schema(description = "Item de pedido retornado pela API")
public class ItemPedidoResponse {

    @Schema(description = "ID único do item de pedido", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "ID do produto no produto-service (pode ser null se não vinculado)",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID produtoId;

    @Schema(description = "Nome do produto (snapshot no momento do pedido)", example = "Teclado Mecânico RGB")
    private String nomeProduto;

    @Schema(description = "SKU do produto (snapshot)", example = "TEC-MEC-RGB")
    private String skuProduto;

    @Schema(description = "Preço unitário no momento do pedido", example = "349.00")
    private BigDecimal precoUnitario;

    @Schema(description = "Quantidade de unidades", example = "2")
    private Integer quantidade;

    @Schema(description = "Subtotal calculado (precoUnitario × quantidade)", example = "698.00")
    private BigDecimal subtotal;
}
