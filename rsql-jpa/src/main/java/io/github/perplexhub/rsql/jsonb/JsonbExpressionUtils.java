package io.github.perplexhub.rsql.jsonb;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;

import java.util.Set;
import java.util.regex.Pattern;

import static io.github.perplexhub.rsql.RSQLOperators.*;

public class JsonbExpressionUtils {    /**
 * The base json type.
 */
    enum BaseJsonType {
        STRING, NUMBER, BOOLEAN, NULL, DATE_TIME, DATE_TIME_TZ
    }

    /**
     * The argument value that holds the value and the base json type.
     */
    record ArgValue(String value, BaseJsonType baseJsonType) {
        public String print(ComparisonOperator operator) {
            return switch (baseJsonType) {
                case STRING -> String.format("\"%s\"", printString(operator));
                case NUMBER, BOOLEAN -> value;
                case NULL -> "null";
                case DATE_TIME, DATE_TIME_TZ -> String.format("\"%s\".datetime()", value);
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
            return value.replaceAll(WILD_CARD_PATTERN.pattern(), ".*");
        }
    }

    /**
     * Interface for argument converters.
     */
    interface ArgConverter {
        boolean accepts(String s);
        ArgValue convert(String s);
    }

    static ArgConverter DATE_TIME_CONVERTER = new ArgConverter() {
        @Override
        public boolean accepts(String s) {
            return ISO_DATE_TIME_PATTERN_TZ.matcher(s).matches()
                    || ISO_DATE_TIME_PATTERN.matcher(s).matches()
                    || ISO_DATE_PATTERN.matcher(s).matches()
                    || ISO_TIME_PATTERN_TZ.matcher(s).matches()
                    || ISO_TIME_PATTERN.matcher(s).matches();
        }

        @Override
        public ArgValue convert(String s) {
            return new ArgValue(s, BaseJsonType.DATE_TIME);
        }
    };

    static ArgConverter NUMBER_CONVERTER = new ArgConverter() {

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


    static ArgConverter BOOLEAN_ARG_CONVERTER = new ArgConverter() {

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

    private static final Pattern ISO_DATE_TIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$");

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private static final Pattern ISO_TIME_PATTERN_TZ = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$");

    private static final Pattern ISO_TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$");

    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$");

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+\\.\\d+$");

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^\\d+$");

    private static final Pattern WILD_CARD_PATTERN = Pattern.compile("\\*");

    static final Set<ComparisonOperator> FORBIDDEN_NEGATION =
            Set.of(IS_NULL, NOT_IN, NOT_LIKE, IGNORE_CASE_NOT_LIKE, NOT_BETWEEN);

    static final Set<ComparisonOperator> NOT_RELEVANT_FOR_CONVERSION =
            Set.of(NOT_NULL, LIKE, IGNORE_CASE);

    static Set<ComparisonOperator> REQUIRE_NO_ARGUMENTS =
            Set.of(NOT_NULL);

    final static Set<ComparisonOperator> REQUIRE_ONE_ARGUMENT =
            Set.of(EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL,
                    LIKE, IGNORE_CASE, IGNORE_CASE_LIKE);

    final static Set<ComparisonOperator> REQUIRE_TWO_ARGUMENTS = Set.of(BETWEEN);

    final static Set<ComparisonOperator> REQUIRE_AT_LEAST_ONE_ARGUMENT = Set.of(IN);

}
