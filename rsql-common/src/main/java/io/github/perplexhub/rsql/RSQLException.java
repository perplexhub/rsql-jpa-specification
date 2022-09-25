package io.github.perplexhub.rsql;

/**
 * Thrown to indicate that generic problems within library. It is a superclass for all exceptions defined here.
 */
public class RSQLException extends RuntimeException {
  public RSQLException() {
    super();
  }

  public RSQLException(String message) {
    super(message);
  }

  public RSQLException(String message, Throwable cause) {
    super(message, cause);
  }

  public RSQLException(Throwable cause) {
    super(cause);
  }
}
