package br.com.infnet.produto.domain;

import br.com.infnet.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value Object que representa uma quantidade inteira de unidades.
 * Garante que o valor nunca é negativo e expõe operações aritméticas seguras.
 */
@Embeddable
public final class Quantidade {

    @Column(nullable = false)
    private int inteiro;

    protected Quantidade() {}

    private Quantidade(int inteiro) {
        this.inteiro = inteiro;
    }

    /** Cria uma {@code Quantidade} com valor maior ou igual a zero. */
    public static Quantidade de(int valor) {
        if (valor < 0)
            throw new DomainException("Quantidade não pode ser negativa");
        return new Quantidade(valor);
    }

    /** Cria uma {@code Quantidade} exigindo valor estritamente positivo. */
    public static Quantidade dePositivo(int valor) {
        if (valor <= 0)
            throw new DomainException("Quantidade deve ser maior que zero");
        return new Quantidade(valor);
    }

    /** Retorna o valor inteiro desta quantidade. */
    public int inteiro() {
        return inteiro;
    }

    /** Alias JavaBeans para compatibilidade com Thymeleaf e Spring EL. */
    public int getInteiro() {
        return inteiro;
    }

    /** Retorna a soma desta quantidade com {@code outra}. */
    public Quantidade somar(Quantidade outra) {
        return new Quantidade(this.inteiro + outra.inteiro);
    }

    /** Retorna o resultado da subtração, garantindo que o resultado não fique negativo. */
    public Quantidade subtrair(Quantidade outra) {
        if (outra.inteiro > this.inteiro)
            throw new DomainException("Quantidade insuficiente para subtração");
        return new Quantidade(this.inteiro - outra.inteiro);
    }

    /** Retorna {@code true} se esta quantidade for estritamente maior que {@code outra}. */
    public boolean maiorQue(Quantidade outra) {
        return this.inteiro > outra.inteiro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantidade q)) return false;
        return inteiro == q.inteiro;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inteiro);
    }

    @Override
    public String toString() {
        return String.valueOf(inteiro);
    }
}
