package br.com.infnet.pedido.dto;

import br.com.infnet.pedido.domain.StatusPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representação pública de um pedido")
public class PedidoResponse {

    @Schema(description = "Identificador único do pedido")
    private UUID id;

    @Schema(description = "Descrição do pedido")
    private String descricao;

    @Schema(description = "Valor do pedido")
    private BigDecimal valor;

    @Schema(description = "Status atual do pedido")
    private StatusPedido status;

    @Schema(description = "Observação ou motivo de contestação")
    private String observacao;

    @Schema(description = "Data e hora de criação")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização")
    private LocalDateTime dataAtualizacao;

    @Schema(description = "Itens do pedido")
    private List<ItemPedidoResponse> itens;

    @Schema(description = "Valor total calculado a partir dos itens (ou valor manual se sem itens)")
    private BigDecimal valorTotal;
}
