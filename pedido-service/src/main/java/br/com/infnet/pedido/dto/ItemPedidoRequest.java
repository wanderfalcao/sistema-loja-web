package br.com.infnet.pedido.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para adicionar um item ao pedido")
public class ItemPedidoRequest {

    @Schema(description = "ID do produto no produto-service (opcional). Se fornecido, nome/SKU/preço são preenchidos automaticamente.",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID produtoId;

    @NotBlank(message = "Nome do produto é obrigatório.")
    @Schema(description = "Nome do produto (snapshot no momento do pedido)", example = "Teclado Mecânico RGB")
    private String nomeProduto;

    @Schema(description = "SKU do produto (snapshot)", example = "TEC-MEC-RGB")
    private String skuProduto;

    @NotNull(message = "Preço unitário é obrigatório.")
    @DecimalMin(value = "0.01", message = "Preço unitário deve ser maior que zero.")
    @Schema(description = "Preço unitário no momento do pedido", example = "349.00")
    private BigDecimal precoUnitario;

    @NotNull(message = "Quantidade é obrigatória.")
    @Min(value = 1, message = "Quantidade mínima é 1.")
    @Schema(description = "Quantidade de unidades", example = "2")
    private Integer quantidade;
}
