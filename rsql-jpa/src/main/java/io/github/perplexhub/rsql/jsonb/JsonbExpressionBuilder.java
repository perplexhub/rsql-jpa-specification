package io.github.perplexhub.rsql.jsonb;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.perplexhub.rsql.RSQLOperators.*;
import static io.github.perplexhub.rsql.jsonb.JsonbExpressionUtils.*;

public class JsonbExpressionBuilder {
    protected static boolean dateTimeSupport = true;

    private final ComparisonOperator operator;
    private final String keyPath;
    private final List<ArgValue> values;

    public JsonbExpressionBuilder(ComparisonOperator operator, String keyPath, List<String> args) {

        //Sanity check
        this.operator = Objects.requireNonNull(operator);
        this.keyPath = Objects.requireNonNull(keyPath);

        if(FORBIDDEN_NEGATION.contains(operator)) {
            throw new IllegalArgumentException("Operator " + operator + " cannot be negated");
        }

        var candidateValues = removeEmptyValuesIfNullCheck(operator, args);

        //List must have at least one value except for IS_NULL and NOT_NULL
        if(candidateValues.isEmpty() && !REQUIRE_NO_ARGUMENTS.contains(operator)) {
            throw new IllegalArgumentException("Values must not be empty");
        }

        //Requires two values
        if(REQUIRE_TWO_ARGUMENTS.contains(operator) && candidateValues.size() != 2) {
            throw new IllegalArgumentException("Operator " + operator + " requires two values");
        }

        //Operators other than IS_NULL, NOT_NULL, BETWEEN, NOT_BETWEEN, IN, NOT_IN require exactly one value
        if(REQUIRE_ONE_ARGUMENT.contains(operator) && candidateValues.size() != 1) {
            throw new IllegalArgumentException("Operator " + operator + " requires one value");
        }

        //Operators IN and NOT_IN require at least one value
        if(REQUIRE_AT_LEAST_ONE_ARGUMENT.contains(operator) && candidateValues.isEmpty()) {
            throw new IllegalArgumentException("Operator " + operator + " requires at least one value");
        }
        //Copy to unmodifiable list
        this.values = findMoreTypes(operator, candidateValues);
    }

    /**
     * Builds a json path expression for a given keyPath and operator.
     * @return the json path expression
     */
    String getJsonPathExpression() {
        List<String> valuesToCompare = values.stream().map(argValue -> argValue.print(operator)).toList();

        String targetPath = String.format("$.%s", removeJsonbReferenceFromKeyPath(keyPath));
        String valueReference = values.stream().filter(argValue -> argValue.baseJsonType().equals(BaseJsonType.DATE_TIME))
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
        List<JsonbExpressionUtils.ArgConverter> argConverters = dateTimeSupport ?
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
        //If the keyPath is empty, we will return an empty string
        if(keyPathParts.isEmpty()) {
            return "";
        }
        //Forget the first part as it represents the jsonb column name
        keyPathParts = keyPathParts.subList(1, keyPathParts.size());
        return String.join(".", keyPathParts);
    }

    /**
     * Returns the template for the given operator.
     * @param operator the operator
     * @param numberOfArguments the number of arguments
     * @return the template
     */
    String operatorToTemplate(ComparisonOperator operator, int numberOfArguments) {
        if(operator.equals(NOT_NULL)) {
            if(numberOfArguments != 0) {
                throw new IllegalArgumentException("Null operator requires no value");
            }
            return "(%s != null)";
        }
        if(operator.equals(EQUAL)) {
            if(numberOfArguments != 1) {
                throw new IllegalArgumentException("Equal operator requires exactly one value");
            }
            return "(%s == %s)";
        }
        if (operator.equals(GREATER_THAN)) {
            if(numberOfArguments != 1) {
                throw new IllegalArgumentException("Greater than operator requires exactly one value");
            }
            return "(%s > %s)";
        }
        if (operator.equals(GREATER_THAN_OR_EQUAL)) {
            if(numberOfArguments != 1) {
                throw new IllegalArgumentException("Greater than or equal operator requires exactly one value");
            }
            return "(%s >= %s)";
        }
        if (operator.equals(LESS_THAN)) {
            if(numberOfArguments != 1) {
                throw new IllegalArgumentException("Less than operator requires exactly one value");
            }
            return "(%s < %s)";
        }
        if (operator.equals(LESS_THAN_OR_EQUAL)) {
            if(numberOfArguments != 1) {
                throw new IllegalArgumentException("Less than or equal operator requires exactly one value");
            }
            return "(%s <= %s)";
        }
        if (operator.equals(LIKE)) {
            if(numberOfArguments != 1) {
                throw new IllegalArgumentException("Like operator requires exactly one value");
            }
            return "(%s like_regex %s)";
        }
        if (operator.equals(IGNORE_CASE)) {
            if(numberOfArguments != 1) {
                throw new IllegalArgumentException("Ignore case operator requires exactly one value");
            }
            return "(%s like_regex %s flag \"i\")";
        }
        if (operator.equals(IGNORE_CASE_LIKE)) {
            if(numberOfArguments != 1) {
                throw new IllegalArgumentException("Ignore case like operator requires exactly one value");
            }
            return "(%s like_regex %s flag \"i\")";
        }
        if (operator.equals(BETWEEN)) {
            if(numberOfArguments != 2) {
                throw new IllegalArgumentException("Between operator requires exactly two values");
            }
            return "(%1$s >= %2$s && %1$s <= %3$s)";
        }
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
        throw new UnsupportedOperationException("Operation " + operator + " is not supported yet");

    }
}
