package br.com.infnet.produto.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Converte {@link Sku} para {@link String} na base de dados e vice-versa. */
@Converter(autoApply = false)
public class SkuConverter implements AttributeConverter<Sku, String> {

    @Override
    public String convertToDatabaseColumn(Sku sku) {
        return sku == null ? null : sku.codigo();
    }

    @Override
    public Sku convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Sku.de(dbData);
    }
}
