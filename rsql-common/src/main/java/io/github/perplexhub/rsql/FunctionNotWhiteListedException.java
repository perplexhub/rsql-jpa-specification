package io.github.perplexhub.rsql;

public class FunctionNotWhiteListedException extends RSQLException {

    public FunctionNotWhiteListedException(String functionName) {
        super(String.format("Function '%s' is not whitelisted", functionName));
    }
}
