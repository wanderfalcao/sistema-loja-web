package br.com.infnet.produto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para ativar promoção em um produto")
public class PromocaoRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "Percentual de desconto deve ser maior que zero")
    @DecimalMax(value = "99.99", message = "Percentual de desconto deve ser menor que 100")
    @Schema(description = "Percentual de desconto (ex.: 20 para 20% de desconto)", example = "20.00")
    private BigDecimal percentualDesconto;

    @Schema(description = "Início da promoção (opcional)")
    private LocalDateTime dataInicio;

    @Schema(description = "Fim da promoção (opcional, deve ser posterior ao início)")
    private LocalDateTime dataFim;
}
