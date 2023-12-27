package io.github.perplexhub.rsql.custom;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class CustomTypeConverter implements AttributeConverter<CustomType, String> {
    @Override
    public String convertToDatabaseColumn(CustomType attribute) {
        return attribute.getValue();
    }

    @Override
    public CustomType convertToEntityAttribute(String dbData) {
        return CustomType.of(dbData);
    }
}
