package io.github.perplexhub.rsql.custom;

import java.util.Objects;
import java.util.Optional;

public class CustomType {

    private String value;

    private CustomType(String value) {
        this.value = value;
    }

    public static CustomType of(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException();
        }
        return new CustomType(value);
    }

    public static Optional<CustomType> ofFailSafe(String value) {
        if (!isValid(value)) {
            return Optional.empty();
        }
        return Optional.of(new CustomType(value));
    }

    private static boolean isValid(String val) {
        return val.startsWith("A");
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomType bic = (CustomType) o;
        return Objects.equals(value, bic.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    //some business methods...
}
