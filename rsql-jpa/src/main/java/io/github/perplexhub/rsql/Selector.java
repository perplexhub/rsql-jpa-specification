package io.github.perplexhub.rsql;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

sealed interface Selector {

    Pattern LONG_PATTERN = Pattern.compile("-?\\d+");
    Pattern DOUBLE_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    Expression<?> getExpression(BiFunction<String, CriteriaBuilder, Expression<?>> columnMapper);

    record ValueSelector(Object value, CriteriaBuilder builder) implements Selector {
        @Override
        public Expression<?> getExpression(BiFunction<String, CriteriaBuilder, Expression<?>> columnMapper) {
            return builder.literal(value);
        }
    }

    record SingleColumnSelector(String column, CriteriaBuilder criteriaBuilder) implements Selector {
        @Override
        public Expression<?> getExpression(BiFunction<String, CriteriaBuilder, Expression<?>> columnMapper) {
            return columnMapper.apply(column, criteriaBuilder);
        }
    }

    record FunctionSelector(String function, Collection<Selector> arguments, CriteriaBuilder builder)
            implements Selector {
        @Override
        public Expression<?> getExpression(BiFunction<String, CriteriaBuilder, Expression<?>> columnMapper) {
            Expression<?>[] expressions = arguments.stream()
                    .map(argument -> argument.getExpression(columnMapper)).toArray(Expression<?>[]::new);
            return builder.function(function, Object.class, expressions);
        }
    }

    static Selector selectorOf(String column, CriteriaBuilder criteriaBuilder) {
        if(column.startsWith("@")) {
            int argStart = column.indexOf('[');
            int argEnd = column.lastIndexOf(']');
            if (argStart > 0 || argEnd > 0) {
                String function = column.substring(1, argStart);
                String argsString = column.substring(argStart + 1, argEnd);
                Collection<Selector> args = Stream.of(argsString.split("\\|"))
                        .map(String::trim)
                        .map(arg -> selectorOf(arg, criteriaBuilder)).toList();
                return new FunctionSelector(function, args, criteriaBuilder);
            }
        } else if (column.startsWith("#")) {
            String value = column.substring(1);
            if(Objects.equals(value, "null")) {
                return new ValueSelector(null, criteriaBuilder);
            }
            //Test if value is a boolean or a number else return the value as string
            Object object = numberFromString(value)
                            .orElseGet(() -> booleanFromString(value)
                            .orElse(value));
            return new ValueSelector(object, criteriaBuilder);

        }
        return new SingleColumnSelector(column, criteriaBuilder);
    }

    static Optional<Object> booleanFromString(String value) {
        if (value.equalsIgnoreCase("true")) {
            return Optional.of(Boolean.TRUE);
        }
        if (value.equalsIgnoreCase("false")) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }

    static Optional<Object> numberFromString(String value) {
        Matcher matcher = LONG_PATTERN.matcher(value);
        if (matcher.matches()) {
            return Optional.of(Long.parseLong(value));
        }
        matcher = DOUBLE_PATTERN.matcher(value);
        if (matcher.matches()) {
            return Optional.of(Double.parseDouble(value));
        }
        return Optional.empty();
    }
}
