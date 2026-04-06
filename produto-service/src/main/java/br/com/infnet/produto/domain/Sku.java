package br.com.infnet.produto.domain;

import br.com.infnet.shared.exception.DomainException;

import java.util.Objects;

/**
 * Value Object que representa o código SKU (Stock Keeping Unit) de um produto.
 * Normaliza automaticamente para maiúsculas e impede valores inválidos.
 */
public final class Sku {

    public static final int MAX_LENGTH = 50;

    private final String codigo;

    private Sku(String codigo) {
        this.codigo = codigo;
    }

    /**
     * Cria um {@code Sku} a partir de uma string, normalizando para maiúsculas.
     *
     * @param raw código bruto; não pode ser nulo ou vazio
     */
    public static Sku de(String raw) {
        if (raw == null || raw.trim().isEmpty())
            throw new DomainException("SKU não pode ser vazio");
        String normalizado = raw.trim().toUpperCase();
        if (normalizado.length() > MAX_LENGTH)
            throw new DomainException("SKU excede " + MAX_LENGTH + " caracteres");
        return new Sku(normalizado);
    }

    /**
     * Gera um {@code Sku} automaticamente a partir do nome do produto,
     * delegando ao {@link SkuGenerator}.
     */
    public static Sku gerar(String nome) {
        return Sku.de(SkuGenerator.fromNome(nome));
    }

    /** Retorna o código SKU como {@link String}. */
    public String codigo() {
        return codigo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sku s)) return false;
        return Objects.equals(codigo, s.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return codigo;
    }
}
