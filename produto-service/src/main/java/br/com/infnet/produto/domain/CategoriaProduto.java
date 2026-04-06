package br.com.infnet.produto.domain;

import java.math.BigDecimal;

/**
 * Categorias fixas de produto.
 * Armazenadas como {@code EnumType.STRING} — sem tabela auxiliar.
 */
public enum CategoriaProduto {

    MONITORES("Monitores") {
        @Override
        public BigDecimal descontoMaximoPermitido() { return new BigDecimal("30"); }
    },
    PERIFERICOS("Periféricos") {
        @Override
        public BigDecimal descontoMaximoPermitido() { return new BigDecimal("40"); }
    },
    ARMAZENAMENTO("Armazenamento") {
        @Override
        public BigDecimal descontoMaximoPermitido() { return new BigDecimal("25"); }
    },
    COMPONENTES("Componentes") {
        @Override
        public BigDecimal descontoMaximoPermitido() { return new BigDecimal("20"); }
    },
    AUDIO_VIDEO("Áudio e Vídeo") {
        @Override
        public BigDecimal descontoMaximoPermitido() { return new BigDecimal("35"); }
    },
    GERAL("Geral") {
        @Override
        public BigDecimal descontoMaximoPermitido() { return new BigDecimal("50"); }
    };

    private final String label;

    CategoriaProduto(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Percentual máximo de desconto permitido para esta categoria. */
    public abstract BigDecimal descontoMaximoPermitido();
}
