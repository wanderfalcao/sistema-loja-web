package br.com.infnet.produto.dto;

import br.com.infnet.produto.domain.TipoOperacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Operação de ajuste de estoque")
public class AjusteEstoqueRequest {

    @NotNull
    @Schema(description = "ENTRADA para repor, SAIDA para consumir", example = "SAIDA")
    private TipoOperacaoEstoque operacao;

    @NotNull
    @Min(1)
    @Schema(description = "Quantidade a movimentar", example = "10")
    private Integer quantidade;
}
