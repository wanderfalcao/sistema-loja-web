package br.com.infnet.produto.domain;

import br.com.infnet.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object que representa um valor monetário em reais (BRL).
 * Garante escala de 2 casas decimais e imutabilidade após criação.
 */
@Embeddable
public final class Dinheiro {

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal quantia;

    protected Dinheiro() {}

    private Dinheiro(BigDecimal quantia) {
        this.quantia = quantia.setScale(2, RoundingMode.HALF_UP);
    }

    /** Cria um {@code Dinheiro} com valor maior ou igual a zero. */
    public static Dinheiro de(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0)
            throw new DomainException("Valor monetário não pode ser negativo");
        return new Dinheiro(valor);
    }

    /** Cria um {@code Dinheiro} exigindo valor estritamente positivo. */
    public static Dinheiro dePositivo(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0)
            throw new DomainException("Valor monetário deve ser maior que zero");
        return new Dinheiro(valor);
    }

    /** Retorna a quantia monetária como {@link BigDecimal}. */
    public BigDecimal quantia() {
        return quantia;
    }

    /** Retorna a soma deste valor com {@code outro}. */
    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(this.quantia.add(outro.quantia));
    }

    /** Retorna o resultado da multiplicação deste valor por {@code fator}. */
    public Dinheiro multiplicar(int fator) {
        return new Dinheiro(this.quantia.multiply(BigDecimal.valueOf(fator)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dinheiro d)) return false;
        return Objects.equals(quantia, d.quantia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantia);
    }

    @Override
    public String toString() {
        return quantia.toPlainString();
    }
}
