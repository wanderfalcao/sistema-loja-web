package br.com.infnet.produto.dto;

import br.com.infnet.produto.domain.CategoriaProduto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Dados para criação ou atualização de produto")
public class ProdutoRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Nome do produto", example = "Monitor 4K")
    private String nome;

    @Size(max = 1000)
    @Schema(description = "Descrição detalhada (opcional)", example = "Monitor 4K UHD 27 polegadas")
    private String descricao;

    @NotNull
    @DecimalMin(value = "0.01", message = "Preco deve ser maior que zero")
    @Schema(description = "Preço do produto em reais", example = "2500.00")
    private BigDecimal preco;

    @NotNull
    @Min(0)
    @Schema(description = "Quantidade em estoque", example = "10")
    private Integer estoque;

    @Schema(description = "Indica se o produto está disponível", example = "true")
    private Boolean ativo;

    @Min(0)
    @Schema(description = "Estoque mínimo para manter o produto ativo (default 0)", example = "5")
    private Integer estoqueMinimo;

    @Schema(description = "Categoria do produto", example = "MONITORES")
    private CategoriaProduto categoria;

    @Size(max = 500)
    @Schema(description = "URL da imagem do produto (opcional)", example = "https://exemplo.com/imagem.jpg")
    private String imagemUrl;
}
