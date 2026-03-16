package br.com.infnet.produto.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Gera códigos SKU a partir do nome do produto.
 *
 * <p>Algoritmo:
 * <ol>
 *   <li>Converte o nome para maiúsculo.</li>
 *   <li>Remove caracteres não-alfanuméricos (acentos, pontuação, etc.).</li>
 *   <li>Normaliza espaços múltiplos.</li>
 *   <li>Pega os primeiros 3 chars de cada uma das primeiras 3 palavras.</li>
 *   <li>Concatena com hífen e adiciona sufixo de 4 chars hexadecimais (UUID aleatório).</li>
 * </ol>
 *
 * <p>Exemplos:
 * <pre>
 *   "Monitor 4K"        → "MON-4K-A3F2"
 *   "Teclado Mecânico"  → "TEC-MEC-NIC-B7D1"
 *   "SSD 1TB"           → "SSD-1TB-C4E9"
 *   "Mouse"             → "MOU-F2A8"
 * </pre>
 */
public final class SkuGenerator {

    private static final int MAX_PALAVRAS = 3;
    private static final int MAX_CHARS_POR_PALAVRA = 3;
    private static final int SUFIXO_COMPRIMENTO = 4;

    private SkuGenerator() {}

    /**
     * Gera um SKU único a partir do {@code nome}.
     *
     * @param nome nome do produto (não nulo)
     * @return SKU no formato {@code PREFIX-XXXX} (maiúsculas, sufixo aleatório)
     * @throws NullPointerException se {@code nome} for nulo
     */
    public static String fromNome(String nome) {
        Objects.requireNonNull(nome, "Nome nao pode ser nulo para gerar SKU");

        String normalizado = nome.toUpperCase()
                .replaceAll("[^A-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] palavras = normalizado.isEmpty() ? new String[]{"PRD"} : normalizado.split(" ");

        StringBuilder prefixo = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_PALAVRAS, palavras.length); i++) {
            String palavra = palavras[i];
            if (palavra.isEmpty()) continue;
            if (prefixo.length() > 0) prefixo.append('-');
            prefixo.append(palavra, 0, Math.min(MAX_CHARS_POR_PALAVRA, palavra.length()));
        }

        String sufixo = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, SUFIXO_COMPRIMENTO)
                .toUpperCase();

        return (prefixo.length() > 0 ? prefixo + "-" : "") + sufixo;
    }
}
