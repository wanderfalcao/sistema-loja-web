package br.com.infnet.produto.domain;

/**
 * Categorias fixas de produto.
 * Armazenadas como {@code EnumType.STRING} — sem tabela auxiliar.
 */
public enum CategoriaProduto {

    MONITORES("Monitores"),
    PERIFERICOS("Periféricos"),
    ARMAZENAMENTO("Armazenamento"),
    COMPONENTES("Componentes"),
    AUDIO_VIDEO("Áudio e Vídeo"),
    GERAL("Geral");

    private final String label;

    CategoriaProduto(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
