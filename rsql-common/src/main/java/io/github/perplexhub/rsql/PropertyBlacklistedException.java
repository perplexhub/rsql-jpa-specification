package io.github.perplexhub.rsql;

/**
 * Thrown to indicate that property in the blacklist.
 */
public class PropertyBlacklistedException extends PropertyAccessControlException {

  public PropertyBlacklistedException(String name, Class<?> type, String message) {
    super(name, type, message);
  }
}
