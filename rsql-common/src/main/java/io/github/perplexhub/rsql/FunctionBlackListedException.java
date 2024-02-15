package io.github.perplexhub.rsql;

public class FunctionBlackListedException extends RSQLException {

    public FunctionBlackListedException(String functionName) {
        super(String.format("Function '%s' is blacklisted", functionName));
    }
}
