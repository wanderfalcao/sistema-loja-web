package br.com.infnet.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstoqueAjusteRequest {
    private TipoOperacaoEstoque operacao;
    private int quantidade;
}
