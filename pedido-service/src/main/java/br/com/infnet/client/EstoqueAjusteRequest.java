package br.com.infnet.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstoqueAjusteRequest {
    private String operacao;
    private int quantidade;
}
