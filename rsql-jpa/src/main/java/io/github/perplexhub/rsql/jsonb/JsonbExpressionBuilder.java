package io.github.perplexhub.rsql.jsonb;


import cz.jirutka.rsql.parser.ast.ComparisonOperator;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.perplexhub.rsql.RSQLOperators.*;
import static io.github.perplexhub.rsql.jsonb.JsonbSupport.*;

/**
 * Builds a jsonb expression for a given keyPath and operator.
 */
public class JsonbExpressionBuilder {

    private final JsonbConfiguration configuration;

    /**
     * The base json type.
     */
    private enum BaseJsonType {
        STRING, NUMBER, BOOLEAN, NULL, DATE_TIME, DATE_TIME_TZ
    }

    /**
     * Interface for argument converters.
     */
    interface ArgConverter {
        boolean accepts(String s);

        ArgValue convert(String s);
    }

    /**
     * The argument value that holds the value and the base json type.
     */
    record ArgValue(String value, BaseJsonType baseJsonType) {
        String print(ComparisonOperator operator) {
            return switch (baseJsonType) {
                case STRING -> String.format("\"%s\"", printString(operator));
                case NUMBER, BOOLEAN -> value;
                case NULL -> "null";
                case DATE_TIME, DATE_TIME_TZ -> String.format("\"%s\".datetime()", value);
            };
        }

        String printString(ComparisonOperator operator) {
            String value = this.value;
            if ((operator.equals(LIKE)
                    || operator.equals(NOT_LIKE)
                    || operator.equals(IGNORE_CASE_LIKE)
                    || operator.equals(IGNORE_CASE_NOT_LIKE))
                    && !value.contains("*")
            ) {
                return String.format(".*%s.*", value);
            }
            return value.replaceAll(WILD_CARD_PATTERN.pattern(), ".*");
        }
    }

    private static final ArgConverter DATE_TIME_CONVERTER = new ArgConverter() {
        @Override
        public boolean accepts(String s) {
            return ISO_DATE_TIME_PATTERN.matcher(s).matches()
                    || ISO_DATE_PATTERN.matcher(s).matches()
                    || ISO_TIME_PATTERN.matcher(s).matches();
        }

        @Override
        public ArgValue convert(String s) {
            return new ArgValue(s, BaseJsonType.DATE_TIME);
        }
    };

    private static final ArgConverter DATE_TIME_CONVERTER_TZ = new ArgConverter() {
        @Override
        public boolean accepts(String s) {
            return ISO_DATE_TIME_PATTERN_TZ.matcher(s).matches()
                    || ISO_TIME_PATTERN_TZ.matcher(s).matches();
        }

        @Override
        public ArgValue convert(String s) {
            return new ArgValue(s, BaseJsonType.DATE_TIME_TZ);
        }
    };

    private static final ArgConverter NUMBER_CONVERTER = new ArgConverter() {

        @Override
        public boolean accepts(String s) {
            return NUMBER_PATTERN.matcher(s).matches()
                    || INTEGER_PATTERN.matcher(s).matches();
        }


        @Override
        public ArgValue convert(String s) {
            return new ArgValue(s, BaseJsonType.NUMBER);
        }
    };

    private static final ArgConverter BOOLEAN_ARG_CONVERTER = new ArgConverter() {

        @Override
        public boolean accepts(String s) {
            return BOOLEAN_PATTERN.matcher(s).matches();
        }

        @Override
        public ArgValue convert(String s) {
            return new ArgValue(s, BaseJsonType.BOOLEAN);
        }
    };

    private static final Pattern ISO_DATE_TIME_PATTERN_TZ = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$");

    private static final Pattern ISO_TIME_PATTERN_TZ = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$");

    private static final Pattern ISO_DATE_TIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$");

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private static final Pattern ISO_TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$");

    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$");

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+\\.\\d+$");

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");

    private static final Pattern WILD_CARD_PATTERN = Pattern.compile("\\*");

    private static final Set<ComparisonOperator> FORBIDDEN_NEGATION =
            Set.of(NOT_EQUAL, IS_NULL, NOT_IN, NOT_LIKE, IGNORE_CASE_NOT_LIKE, NOT_BETWEEN);

    private static final Set<ComparisonOperator> NOT_RELEVANT_FOR_CONVERSION =
            Set.of(NOT_NULL, LIKE, IGNORE_CASE, IGNORE_CASE_LIKE);

    private static final Set<ComparisonOperator> REQUIRE_NO_ARGUMENTS =
            Set.of(NOT_NULL);

    private static final Set<ComparisonOperator> REQUIRE_ONE_ARGUMENT =
            Set.of(EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL,
                    LIKE, IGNORE_CASE, IGNORE_CASE_LIKE);

    private static final Set<ComparisonOperator> REQUIRE_TWO_ARGUMENTS = Set.of(BETWEEN);

    private static final Set<ComparisonOperator> REQUIRE_AT_LEAST_ONE_ARGUMENT = Set.of(IN);

    private static final Map<ComparisonOperator, String> COMPARISON_TEMPLATE = Map.ofEntries(
            Map.entry(NOT_NULL, "(%s != null)"),
            Map.entry(EQUAL, "(%s == %s)"),
            Map.entry(GREATER_THAN, "(%s > %s)"),
            Map.entry(GREATER_THAN_OR_EQUAL, "(%s >= %s)"),
            Map.entry(LESS_THAN, "(%s < %s)"),
            Map.entry(LESS_THAN_OR_EQUAL, "(%s <= %s)"),
            Map.entry(LIKE, "(%s like_regex %s)"),
            Map.entry(IGNORE_CASE, "(%s like_regex %s flag \"i\")"),
            Map.entry(IGNORE_CASE_LIKE, "(%s like_regex %s flag \"i\")"),
            Map.entry(BETWEEN, "(%1$s >= %2$s && %1$s <= %3$s)")
    );

    private final ComparisonOperator operator;
    private final String keyPath;
    private final List<ArgValue> values;

    JsonbExpressionBuilder(ComparisonOperator operator, String keyPath, List<String> args) {
        this(operator, keyPath, args, JsonbConfiguration.DEFAULT);
    }

    JsonbExpressionBuilder(ComparisonOperator operator, String keyPath, List<String> args, JsonbConfiguration configuration) {
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
        this.configuration = configuration;
        this.values = findMoreTypes(operator, candidateValues);
    }

    /**
     * Builds a json jsonbPath expression for a given keyPath and operator.
     * @return the json jsonbPath expression
     */
    public JsonbPathExpression getJsonPathExpression() {
        List<String> valuesToCompare = values.stream().map(argValue -> argValue.print(operator)).toList();
        String targetPath = String.format("$.%s", removeJsonbReferenceFromKeyPath(keyPath));
        boolean isDataTime = values.stream().anyMatch(argValue -> argValue.baseJsonType().equals(BaseJsonType.DATE_TIME));
        boolean isDateTimeTz = values.stream().anyMatch(argValue -> argValue.baseJsonType().equals(BaseJsonType.DATE_TIME_TZ));
        String valueReference = values.stream()
                .filter(v -> isDataTime || isDateTimeTz)
                .findFirst()
                .map(v -> "@.datetime()")
                .orElse("@");
        ComparisonOperator realOperator = transformEqualsToLike(operator, valuesToCompare);
        String comparisonTemplate = operatorToTemplate(realOperator, valuesToCompare.size());
        List<String> templateArguments = new ArrayList<>();
        templateArguments.add(valueReference);
        templateArguments.addAll(valuesToCompare);
        var function = isDateTimeTz ? configuration.pathExistsTz() : configuration.pathExists();
        var expression = String.format("%s ? %s", targetPath, String.format(comparisonTemplate, templateArguments.toArray()));
        return new JsonbPathExpression(function, expression);
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

        List<ArgConverter> argConverters = configuration.useDateTime() ?
                List.of(DATE_TIME_CONVERTER, DATE_TIME_CONVERTER_TZ, NUMBER_CONVERTER, BOOLEAN_ARG_CONVERTER)
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
