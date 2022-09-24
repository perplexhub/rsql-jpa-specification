package io.github.perplexhub.rsql;

/**
 * Thrown to indicate generic problem with entity property (e.g. not existing, forbidden).
 */
public class PropertyException extends RSQLException {
  private final String name;
  private final Class<?> type;

  public PropertyException(String name, Class<?> type) {
    this(name, type, (String) null);
  }

  public PropertyException(String name, Class<?> type, String message) {
    super(message);

    this.name = name;
    this.type = type;
  }

  public PropertyException(String name, Class<?> type, Throwable cause) {
    super(cause);

    this.name = name;
    this.type = type;
  }

  /**
   * Returns the problematic property name.
   *
   * @return the problematic property name.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns class that (not)contains property with {@link #getName()}.
   *
   * @return the class that (not)contains property with {@link #getName()}
   */
  public Class<?> getType() {
    return type;
  }
}
