package io.github.perplexhub.rsql;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import lombok.val;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.perplexhub.rsql.RSQLOperators.*;

public class PostgresJsonPathExpressionBuilder {

    protected static boolean dateTimeSupport = true;

     private final static Set<ComparisonOperator> REQUIRE_NO_ARGUMENTS = Set.of(IS_NULL, NOT_NULL);

    private final static Set<ComparisonOperator> REQUIRE_ONE_ARGUMENT = Set.of(EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, LIKE, NOT_LIKE, IGNORE_CASE, IGNORE_CASE_LIKE, IGNORE_CASE_NOT_LIKE);

    private final static Set<ComparisonOperator> REQUIRE_TWO_ARGUMENTS = Set.of(BETWEEN, NOT_BETWEEN);


    private final ComparisonOperator operator;
    private final String keyPath;
    private final List<ArgValue> values;

    public PostgresJsonPathExpressionBuilder(ComparisonOperator operator, String keyPath, List<String> args) {

        //Sanity check
        this.operator = Objects.requireNonNull(operator);
        this.keyPath = Objects.requireNonNull(keyPath);

        val values = sanitizeValues(operator, args);

        //Values must not be null
        if(values == null) {
            throw new IllegalArgumentException("Values must not be null");
        }

        //List must have at least one value except for IS_NULL and NOT_NULL
        if(values.isEmpty() && !REQUIRE_NO_ARGUMENTS.contains(operator)) {
            throw new IllegalArgumentException("Values must not be empty");
        }

        //Requires two values
        if(REQUIRE_TWO_ARGUMENTS.contains(operator) && values.size() != 2) {
            throw new IllegalArgumentException("Operator " + operator + " requires two values");
        }

        //Operators other than IS_NULL, NOT_NULL, BETWEEN, NOT_BETWEEN, IN, NOT_IN require exactly one value
        if(REQUIRE_ONE_ARGUMENT.contains(operator) && values.size() != 1) {
            throw new IllegalArgumentException("Operator " + operator + " requires one value");
        }
        //Copy to unmodifiable list
        this.values = findMoreTypes(values);
    }

    private List<String> sanitizeValues(ComparisonOperator operator, List<String> args) {
        if(operator.equals(IS_NULL) || operator.equals(NOT_NULL)) {
            return Collections.emptyList();
        }
        return args;
    }

    private List<ArgValue> findMoreTypes(List<String> values) {
        List<ArgConverter> argConverters = dateTimeSupport ?
                List.of(DATE_TIME_CONVERTER, NUMBER_CONVERTER, BOOLEAN_ARG_CONVERTER)
                : List.of(NUMBER_CONVERTER, BOOLEAN_ARG_CONVERTER);
        Optional<ArgConverter> candidateConverter = argConverters.stream()
                .filter(argConverter -> values.stream().allMatch(argConverter::accepts))
                .findFirst();
        return candidateConverter.map(argConverter -> values.stream()
                .map(argConverter::convert).toList())
                .orElseGet(() -> values.stream().map(s -> new ArgValue(s, BaseJsonType.STRING)).toList());
    }

    String getJsonPathTest() {
        List<String> valuesToCompare = values.stream().map(argValue -> argValue.print(operator)).toList();

        String targetPath = String.format("$.%s", removeJsonbReferenceFromKeyPath(keyPath));
        String valueReference = values.stream().filter(argValue -> argValue.baseJsonType.equals(BaseJsonType.DATE_TIME))
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


    enum BaseJsonType {
        STRING, NUMBER, BOOLEAN, NULL, DATE_TIME
    }

    record ArgValue(String value, BaseJsonType baseJsonType) {
        public String print(ComparisonOperator operator) {
            return switch (baseJsonType) {
                case STRING -> String.format("\"%s\"", printString(operator));
                case NUMBER, BOOLEAN -> value;
                case NULL -> "null";
                case DATE_TIME -> String.format("\"%s\".datetime()", value);
            };
        }

        public String printString(ComparisonOperator operator) {
            String value = this.value;
            if((operator.equals(LIKE)
                    || operator.equals(NOT_LIKE)
                    || operator.equals(IGNORE_CASE_LIKE)
                    || operator.equals(IGNORE_CASE_NOT_LIKE))
                    && !value.contains("*")
            ) {
                return String.format(".*%s.*", value);
            }
            return value.replaceAll("\\*", ".*");
        }
    }

    interface ArgConverter {
        boolean accepts(String s);
        ArgValue convert(String s);
    }

    static ArgConverter DATE_TIME_CONVERTER = new ArgConverter() {
        @Override
        public boolean accepts(String s) {
            return s.matches("^\\d{2}:\\d{2}:\\d{2}$")
                    || s.matches("^\\d{4}-\\d{2}-\\d{2}$")
                    || s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$");
        }

        @Override
        public ArgValue convert(String s) {
            return new ArgValue(s, BaseJsonType.DATE_TIME);
        }
    };

    static ArgConverter NUMBER_CONVERTER = new ArgConverter() {

        @Override
        public boolean accepts(String s) {
            return s.matches("^\\d+\\.\\d+$")
                    || s.matches("^\\d+$");
        }


        @Override
        public ArgValue convert(String s) {
            return new ArgValue(s, BaseJsonType.NUMBER);
        }
    };


    static ArgConverter BOOLEAN_ARG_CONVERTER = new ArgConverter() {

        @Override
        public boolean accepts(String s) {
            return s.matches("^(true|false)$");
        }

        @Override
        public ArgValue convert(String s) {
            return new ArgValue(s, BaseJsonType.BOOLEAN);
        }
    };
}
