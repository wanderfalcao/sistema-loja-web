package br.com.infnet.pedido.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para contestar um pedido concluído")
public class ContestarRequest {

    @NotBlank(message = "Motivo da contestação não pode ser vazio.")
    @Schema(description = "Motivo da contestação", example = "Produto com defeito")
    private String motivo;
}
