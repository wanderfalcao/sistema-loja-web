package br.com.infnet.produto.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class SkuGeneratorTest {

    // ── Formato e prefixo ─────────────────────────────────────────────────────

    @ParameterizedTest(name = "'{0}' deve começar com '{1}'")
    @CsvSource({
        "Monitor,           MON-",
        "Monitor 4K,        MON-4K-",
        "Monitor 4K Ultra,  MON-4K-ULT-",
        "SSD 1TB,           SSD-1TB-",
        "Mouse Gamer Pro,   MOU-GAM-PRO-"
    })
    void deveGerarPrefixoCorreto(String nome, String prefixoEsperado) {
        String sku = SkuGenerator.fromNome(nome);
        assertThat(sku).startsWith(prefixoEsperado);
    }

    @Test
    void deveTerminarComSufixoDe4Chars() {
        String sku = SkuGenerator.fromNome("Monitor");
        // formato: PREFIX-XXXX onde XXXX é 4 chars hex maiúsculo
        assertThat(sku).matches(".*-[A-Z0-9]{4}$");
    }

    @Test
    void deveGerarFormatoCompletoCorreto() {
        String sku = SkuGenerator.fromNome("Monitor 4K");
        // MON-4K-XXXX
        assertThat(sku).matches("MON-4K-[A-Z0-9]{4}");
    }

    @Test
    void deveLimitarAoMaximoDeTresPalavras() {
        // 5 palavras → apenas as 3 primeiras contribuem para o prefixo
        String sku = SkuGenerator.fromNome("Mouse Gamer Pro Sem Fio");
        assertThat(sku).matches("MOU-GAM-PRO-[A-Z0-9]{4}");
    }

    // ── Unicidade ─────────────────────────────────────────────────────────────

    @Test
    void deveProduzirSkusDistintosParaMesmoNome() {
        String sku1 = SkuGenerator.fromNome("Monitor");
        String sku2 = SkuGenerator.fromNome("Monitor");

        // Com 16^4 = 65536 possibilidades, colisão é impraticável
        assertThat(sku1).isNotEqualTo(sku2);
    }

    // ── Normalização ─────────────────────────────────────────────────────────

    @Test
    void deveMaiusculizarResultado() {
        String sku = SkuGenerator.fromNome("monitor");
        assertThat(sku).matches("MON-[A-Z0-9]{4}");
    }

    @Test
    void deveRemoverCaracteresEspeciais() {
        // "Notebook!" → "NOT-XXXX" (! é removido)
        String sku = SkuGenerator.fromNome("Notebook!");
        assertThat(sku).matches("NOT-[A-Z0-9]{4}");
    }

    @Test
    void deveNormalizarEspacosMultiplos() {
        String sku = SkuGenerator.fromNome("  Monitor   4K  ");
        assertThat(sku).matches("MON-4K-[A-Z0-9]{4}");
    }

    @Test
    void deveUsarPrimeiros3CharsDeWordCurta() {
        // "SSD" tem 3 chars, "AB" tem 2 → usa "AB" inteiro
        String sku = SkuGenerator.fromNome("AB CD");
        assertThat(sku).matches("AB-CD-[A-Z0-9]{4}");
    }

    // ── Respeita MAX_SKU ──────────────────────────────────────────────────────

    @Test
    void deveProduzirSkuDentroDoLimiteMaximo() {
        // Nome muito longo — prefixo ainda será no máximo 3x3 chars = 9 + hifens + sufixo 4 = 16 chars max
        String sku = SkuGenerator.fromNome("Computador Processador Ultra");
        assertThat(sku.length()).isLessThanOrEqualTo(Produto.MAX_SKU);
    }

    // ── Entradas extremas ────────────────────────────────────────────────────

    @Test
    void deveLancarNullPointerParaNomeNulo() {
        assertThatThrownBy(() -> SkuGenerator.fromNome(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Nome nao pode ser nulo");
    }

    @Test
    void deveUsarPRDParaNomeSemCaracteresValidos() {
        // Após remoção de caracteres especiais, normalizado fica vazio → usa fallback "PRD"
        String sku = SkuGenerator.fromNome("!!!");
        assertThat(sku).matches("PRD-[A-Z0-9]{4}");
    }
}
