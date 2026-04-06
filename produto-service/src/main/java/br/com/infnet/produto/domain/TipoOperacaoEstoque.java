package br.com.infnet.produto.domain;

import br.com.infnet.shared.exception.DomainException;

public enum TipoOperacaoEstoque {

    ENTRADA {
        @Override
        public Quantidade aplicar(Quantidade atual, Quantidade delta) {
            return atual.somar(delta);
        }

        @Override
        public void validarAntesDeAplicar(boolean ativo) {
        }
    },

    SAIDA {
        @Override
        public Quantidade aplicar(Quantidade atual, Quantidade delta) {
            if (atual.inteiro() < delta.inteiro())
                throw new DomainException("Estoque insuficiente. Disponível: " + atual.inteiro());
            return atual.subtrair(delta);
        }

        @Override
        public void validarAntesDeAplicar(boolean ativo) {
            if (!ativo)
                throw new DomainException("Saída de estoque não permitida para produto inativo.");
        }
    };

    /** Aplica a operação de estoque e retorna o novo valor. */
    public abstract Quantidade aplicar(Quantidade atual, Quantidade delta);

    /** Valida pré-condições antes de aplicar a operação. */
    public abstract void validarAntesDeAplicar(boolean ativo);
}
