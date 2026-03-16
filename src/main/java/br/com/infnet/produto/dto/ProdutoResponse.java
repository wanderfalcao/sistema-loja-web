package br.com.infnet.produto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados de retorno de produto")
public class ProdutoResponse {

    @Schema(description = "Identificador único", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Nome do produto", example = "Monitor 4K")
    private String nome;

    @Schema(description = "Descrição detalhada", example = "Monitor 4K UHD 27 polegadas")
    private String descricao;

    @Schema(description = "Código SKU", example = "MON-4K-001")
    private String sku;

    @Schema(description = "Preço em reais", example = "2500.00")
    private BigDecimal preco;

    @Schema(description = "Preço com desconto (nulo quando sem promoção ativa)", example = "2000.00")
    private BigDecimal precoComDesconto;

    @Schema(description = "Percentual de desconto da promoção ativa", example = "20.00")
    private BigDecimal percentualDesconto;

    @Schema(description = "Início da promoção ativa")
    private LocalDateTime promocaoInicio;

    @Schema(description = "Fim da promoção ativa")
    private LocalDateTime promocaoFim;

    @Schema(description = "Quantidade em estoque", example = "10")
    private Integer estoque;

    @Schema(description = "Estoque mínimo para manter o produto ativo", example = "5")
    private Integer estoqueMinimo;

    @Schema(description = "Produto ativo/disponível", example = "true")
    private Boolean ativo;

    @Schema(description = "Nome da categoria do produto")
    private String categoriaNome;

    @Schema(description = "Data de criação do registro")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data da última atualização")
    private LocalDateTime dataAtualizacao;
}
