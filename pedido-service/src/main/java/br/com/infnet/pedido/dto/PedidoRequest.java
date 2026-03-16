package br.com.infnet.pedido.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para criar ou atualizar um pedido")
public class PedidoRequest {

    @NotBlank(message = "Descrição não pode ser vazia.")
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
    @Schema(description = "Descrição do pedido", example = "Pedido de material de escritório")
    private String descricao;

    @NotNull(message = "Valor é obrigatório.")
    @DecimalMin(value = "0.01", message = "Valor deve ser no mínimo R$ 0,01.")
    @Schema(description = "Valor do pedido", example = "49.90")
    private BigDecimal valor;

    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres.")
    @Schema(description = "Observação opcional", example = "Entregar pela manhã")
    private String observacao;
}
