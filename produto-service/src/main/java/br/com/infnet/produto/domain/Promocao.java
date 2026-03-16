package br.com.infnet.produto.domain;

import br.com.infnet.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Embeddable
public class Promocao {

    private static final BigDecimal CEM = new BigDecimal("100");

    @Column(name = "promo_percentual", precision = 5, scale = 2)
    private BigDecimal percentualDesconto;

    @Column(name = "promo_preco", precision = 10, scale = 2)
    private BigDecimal precoComDesconto;

    @Column(name = "promo_inicio")
    private LocalDateTime inicio;

    @Column(name = "promo_fim")
    private LocalDateTime fim;

    /** Construtor sem argumentos exigido pelo JPA. */
    public Promocao() {}

    /**
     * Factory com validação embutida. Calcula {@code precoComDesconto} automaticamente.
     *
     * @param percentual percentual de desconto (exclusivo: 0 &lt; x &lt; 100)
     * @param inicio     início da promoção (pode ser nulo)
     * @param fim        fim da promoção (deve ser posterior ao início quando ambos informados)
     * @param precoBase  preço original do produto
     */
    public static Promocao criar(BigDecimal percentual, LocalDateTime inicio,
                                  LocalDateTime fim, BigDecimal precoBase) {
        if (percentual == null
                || percentual.compareTo(BigDecimal.ZERO) <= 0
                || percentual.compareTo(CEM) >= 0) {
            throw new DomainException("Percentual de desconto deve ser maior que 0 e menor que 100");
        }
        if (fim != null && inicio != null && !fim.isAfter(inicio)) {
            throw new DomainException("Data de fim da promocao deve ser posterior ao inicio");
        }

        Promocao p = new Promocao();
        p.percentualDesconto = percentual;
        p.precoComDesconto = precoBase
                .multiply(BigDecimal.ONE.subtract(percentual.divide(CEM)))
                .setScale(2, RoundingMode.HALF_UP);
        p.inicio = inicio;
        p.fim = fim;
        return p;
    }

    public BigDecimal getPercentualDesconto() { return percentualDesconto; }
    public BigDecimal getPrecoComDesconto()   { return precoComDesconto; }
    public LocalDateTime getInicio()           { return inicio; }
    public LocalDateTime getFim()              { return fim; }
}
