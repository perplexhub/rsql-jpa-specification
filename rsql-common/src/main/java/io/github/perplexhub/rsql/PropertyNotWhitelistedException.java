package io.github.perplexhub.rsql;

/**
 * Thrown to indicate that property not in the whitelist.
 */
public class PropertyNotWhitelistedException extends PropertyAccessControlException {

  public PropertyNotWhitelistedException(String name, Class<?> type, String message) {
    super(name, type, message);
  }
}
