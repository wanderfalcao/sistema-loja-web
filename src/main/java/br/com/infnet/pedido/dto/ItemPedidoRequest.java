package br.com.infnet.pedido.dto;

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
public class ItemPedidoRequest {

    private UUID produtoId;

    @NotBlank(message = "Nome do produto é obrigatório.")
    private String nomeProduto;

    private String skuProduto;

    @NotNull(message = "Preço unitário é obrigatório.")
    @DecimalMin(value = "0.01", message = "Preço unitário deve ser maior que zero.")
    private BigDecimal precoUnitario;

    @NotNull(message = "Quantidade é obrigatória.")
    @Min(value = 1, message = "Quantidade mínima é 1.")
    private Integer quantidade;
}
