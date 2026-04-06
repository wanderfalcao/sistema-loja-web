package br.com.infnet.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoInfo {
    private UUID id;
    private String nome;
    private String sku;
    private BigDecimal preco;
    private boolean ativo;
    private int estoque;
}
