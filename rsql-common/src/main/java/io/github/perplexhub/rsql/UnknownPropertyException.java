package io.github.perplexhub.rsql;

/**
 * Thrown to indicate that RSQL query contains unknown field.
 */
public class UnknownPropertyException extends PropertyException {

  public UnknownPropertyException(String name, Class<?> type) {
    super(name, type, String.format("Unknown property: %s from entity %s", name, type.getName()));
  }
}
