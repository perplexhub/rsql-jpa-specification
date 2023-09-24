package io.github.perplexhub.rsql.jsonb;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.perplexhub.rsql.RSQLOperators.*;
import static io.github.perplexhub.rsql.jsonb.JsonbExpressionConstants.*;
import static io.github.perplexhub.rsql.jsonb.JsonbPathSupport.dateTimeSupport;

/**
 * Builds a jsonb expression for a given keyPath and operator.
 */
public class JsonbExpressionBuilder {

    private final ComparisonOperator operator;
    private final String keyPath;
    private final List<ArgValue> values;

    public JsonbExpressionBuilder(ComparisonOperator operator, String keyPath, List<String> args) {
        this.operator = Objects.requireNonNull(operator);
        this.keyPath = Objects.requireNonNull(keyPath);
        if(FORBIDDEN_NEGATION.contains(operator)) {
            throw new IllegalArgumentException("Operator " + operator + " cannot be negated");
        }
        var candidateValues = removeEmptyValuesIfNullCheck(operator, args);
        if(candidateValues.isEmpty() && !REQUIRE_NO_ARGUMENTS.contains(operator)) {
            throw new IllegalArgumentException("Values must not be empty");
        }
        if(REQUIRE_TWO_ARGUMENTS.contains(operator) && candidateValues.size() != 2) {
            throw new IllegalArgumentException("Operator " + operator + " requires two values");
        }
        if(REQUIRE_ONE_ARGUMENT.contains(operator) && candidateValues.size() != 1) {
            throw new IllegalArgumentException("Operator " + operator + " requires one value");
        }
        if(REQUIRE_AT_LEAST_ONE_ARGUMENT.contains(operator) && candidateValues.isEmpty()) {
            throw new IllegalArgumentException("Operator " + operator + " requires at least one value");
        }
        this.values = findMoreTypes(operator, candidateValues);
    }

    /**
     * Builds a json path expression for a given keyPath and operator.
     * @return the json path expression
     */
    String getJsonPathExpression() {
        List<String> valuesToCompare = values.stream().map(argValue -> argValue.print(operator)).toList();
        String targetPath = String.format("$.%s", removeJsonbReferenceFromKeyPath(keyPath));
        String valueReference = values.stream()
                .filter(argValue -> argValue.baseJsonType().equals(BaseJsonType.DATE_TIME))
                .findFirst()
                .map(v -> "@.datetime()")
                .orElse("@");
        ComparisonOperator realOperator = transformEqualsToLike(operator, valuesToCompare);
        String comparisonTemplate = operatorToTemplate(realOperator, valuesToCompare.size());
        List<String> templateArguments = new ArrayList<>();
        templateArguments.add(valueReference);
        templateArguments.addAll(valuesToCompare);
        return String.format("%s ? %s", targetPath, String.format(comparisonTemplate, templateArguments.toArray()));
    }

    /**
     * If the operator is NOT_NULL, we will remove all values.
     */
    private List<String> removeEmptyValuesIfNullCheck(ComparisonOperator operator, List<String> args) {
        if(operator.equals(NOT_NULL)) {
            return Collections.emptyList();
        }
        return args;
    }

    /**
     * Try to find a more specific type for the given values.
     * We will keep the original value if we cannot find a more specific type for all values.
     */
    private List<ArgValue> findMoreTypes(ComparisonOperator operator, List<String> values) {
        if(NOT_RELEVANT_FOR_CONVERSION.contains(operator)) {
            return values.stream().map(s -> new ArgValue(s, BaseJsonType.STRING)).toList();
        }

        List<JsonbExpressionConstants.ArgConverter> argConverters = dateTimeSupport ?
                List.of(DATE_TIME_CONVERTER, NUMBER_CONVERTER, BOOLEAN_ARG_CONVERTER)
                : List.of(NUMBER_CONVERTER, BOOLEAN_ARG_CONVERTER);
        Optional<ArgConverter> candidateConverter = argConverters.stream()
                .filter(argConverter -> values.stream().allMatch(argConverter::accepts))
                .findFirst();

        return candidateConverter.map(argConverter -> values.stream()
                        .map(argConverter::convert).toList())
                .orElseGet(() -> values.stream().map(s -> new ArgValue(s, BaseJsonType.STRING)).toList());
    }

    /**
     * If the operator is EQUAL and one of the values contains a wildcard, we will transform the operator to LIKE.
     */
    private ComparisonOperator transformEqualsToLike(ComparisonOperator operator, List<String> valuesToCompare) {
        boolean hasWildcard = valuesToCompare.stream().anyMatch(s -> s.contains("*"));
        if(!hasWildcard) {
            return operator;
        }
        if(operator.equals(EQUAL)) {
            return LIKE;
        }
        return operator;
    }

    /**
     * Removes the jsonb reference from the keyPath.
     * @param keyPath the keyPath
     * @return the keyPath without the jsonb reference
     */
    String removeJsonbReferenceFromKeyPath(String keyPath) {
        List<String> keyPathParts = Arrays.asList(keyPath.split("\\."));
        if(keyPathParts.isEmpty()) {
            return "";
        }
        //Forget the first part as it represents the jsonb column name
        keyPathParts = keyPathParts.subList(1, keyPathParts.size());
        return String.join(".", keyPathParts);
    }

    /**
     * Returns the String template for the given operator.
     * @param operator the operator
     * @param numberOfArguments the number of arguments
     * @return the template
     */
    String operatorToTemplate(ComparisonOperator operator, int numberOfArguments) {
        if (operator.equals(IN)) {
            if(numberOfArguments < 1) {
                throw new IllegalArgumentException("In operator requires at least one value");
            }
            var orChain = new ArrayList<String>();
            for (int i = 1; i <= numberOfArguments; i++) {
                orChain.add("%1$s == %" + (i + 1) + "$s");
            }
            return orChain.stream()
                    .collect(Collectors.joining(" || ", "(", ")"));
        }
        return Optional.ofNullable(COMPARISON_TEMPLATE.get(operator))
                .orElseThrow(() -> new UnsupportedOperationException(operator + " is not supported yet"));
    }
}
