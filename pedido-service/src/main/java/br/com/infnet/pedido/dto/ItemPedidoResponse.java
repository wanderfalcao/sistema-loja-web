package br.com.infnet.pedido.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoResponse {

    private UUID id;
    private UUID produtoId;
    private String nomeProduto;
    private String skuProduto;
    private BigDecimal precoUnitario;
    private Integer quantidade;
    private BigDecimal subtotal;
}
