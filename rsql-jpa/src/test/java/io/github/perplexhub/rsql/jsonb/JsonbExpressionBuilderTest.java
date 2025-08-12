package io.github.perplexhub.rsql.jsonb;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.perplexhub.rsql.RSQLOperators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JsonbExpressionBuilderTest {

    @ParameterizedTest
    @MethodSource("data")
    void testJsonbPathExpression(ComparisonOperator operator, String keyPath, List<String> arguments, String expectedJsonbFunction, String expectedJsonbPath) {
        JsonbSupport.DATE_TIME_SUPPORT = false;
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(operator, keyPath, arguments);
        var expression = builder.getJsonPathExpression();
        assertEquals(expectedJsonbFunction, expression.jsonbFunction());
        assertEquals(expectedJsonbPath, expression.jsonbPath());
    }

    @ParameterizedTest
    @MethodSource("temporal")
    void testJsonbPathExpressionWithTemporal(ComparisonOperator operator, String keyPath, List<String> arguments, String expectedJsonbFunction, String expectedJsonbPath) {
        JsonbSupport.DATE_TIME_SUPPORT = true;
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(operator, keyPath, arguments);
        var expression = builder.getJsonPathExpression();
        assertEquals(expectedJsonbFunction, expression.jsonbFunction());
        assertEquals(expectedJsonbPath, expression.jsonbPath());
    }

    @ParameterizedTest
    @MethodSource("customized")
    void testJsonbPathExpressionCustomized(ComparisonOperator operator, String keyPath, List<String> arguments, String expectedJsonbFunction, String expectedJsonbPath) {
        String jsonbPathExists = JsonbSupport.JSONB_PATH_EXISTS;
        String jsonbPathExistsTz = JsonbSupport.JSONB_PATH_EXISTS_TZ;
        try {
            JsonbSupport.JSONB_PATH_EXISTS = "my_jsonb_path_exists";
            JsonbSupport.JSONB_PATH_EXISTS_TZ = "my_jsonb_path_exists_tz";
            JsonbSupport.DATE_TIME_SUPPORT = true;
            JsonbExpressionBuilder builder = new JsonbExpressionBuilder(operator, keyPath, arguments);
            var expression = builder.getJsonPathExpression();
            assertEquals(expectedJsonbFunction, expression.jsonbFunction());
            assertEquals(expectedJsonbPath, expression.jsonbPath());
        } catch (Exception e) {
            throw e;
        } finally {
            JsonbSupport.JSONB_PATH_EXISTS = jsonbPathExists;
            JsonbSupport.JSONB_PATH_EXISTS_TZ = jsonbPathExistsTz;
        }
    }

    static Stream<Arguments> data() {
        return Stream.of(
                allOperators(),
                conversion(),
                null
        ).filter(Objects::nonNull).flatMap(s -> s);
    }

    static Stream<Arguments> allOperators() {
        return Stream.of(
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("value"), "jsonb_path_exists", "$.equal_key ? (@ == \"value\")"),
                arguments(RSQLOperators.GREATER_THAN, "json.greater_than_key", Collections.singletonList("value"), "jsonb_path_exists", "$.greater_than_key ? (@ > \"value\")"),
                arguments(RSQLOperators.GREATER_THAN_OR_EQUAL, "json.greater_than_or_equal_key", Collections.singletonList("value"), "jsonb_path_exists", "$.greater_than_or_equal_key ? (@ >= \"value\")"),
                arguments(RSQLOperators.LESS_THAN, "json.less_than_key", Collections.singletonList("value"), "jsonb_path_exists", "$.less_than_key ? (@ < \"value\")"),
                arguments(RSQLOperators.LESS_THAN_OR_EQUAL, "json.less_than_or_equal_key", Collections.singletonList("value"), "jsonb_path_exists", "$.less_than_or_equal_key ? (@ <= \"value\")"),
                arguments(RSQLOperators.IN, "json.in_key", List.of("value1", "value2"), "jsonb_path_exists", "$.in_key ? (@ == \"value1\" || @ == \"value2\")"),
                arguments(RSQLOperators.NOT_NULL, "json.not_null_key", Collections.singletonList("value"), "jsonb_path_exists", "$.not_null_key ? (@ != null)"),
                arguments(RSQLOperators.LIKE, "json.like_key", Collections.singletonList("value"), "jsonb_path_exists", "$.like_key ? (@ like_regex \".*value.*\")"),
                arguments(RSQLOperators.IGNORE_CASE, "json.ignore_case_key", Collections.singletonList("value"), "jsonb_path_exists", "$.ignore_case_key ? (@ like_regex \"value\" flag \"i\")"),
                arguments(RSQLOperators.IGNORE_CASE_LIKE, "json.ignore_case_like_key", Collections.singletonList("value"), "jsonb_path_exists", "$.ignore_case_like_key ? (@ like_regex \".*value.*\" flag \"i\")"),
                arguments(RSQLOperators.BETWEEN, "json.between_key", List.of("1", "2"), "jsonb_path_exists", "$.between_key ? (@ >= 1 && @ <= 2)"),
                null
        ).filter(Objects::nonNull);
    }

    static Stream<Arguments> conversion() {
        return Stream.of(
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("a"), "jsonb_path_exists", "$.equal_key ? (@ == \"a\")"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1"), "jsonb_path_exists", "$.equal_key ? (@ == 1)"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1.1"), "jsonb_path_exists", "$.equal_key ? (@ == 1.1)"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("true"), "jsonb_path_exists", "$.equal_key ? (@ == true)"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("false"), "jsonb_path_exists", "$.equal_key ? (@ == false)"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("null"), "jsonb_path_exists", "$.equal_key ? (@ == \"null\")"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1.1.1"), "jsonb_path_exists", "$.equal_key ? (@ == \"1.1.1\")"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1,1"), "jsonb_path_exists", "$.equal_key ? (@ == \"1,1\")"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1 1"), "jsonb_path_exists", "$.equal_key ? (@ == \"1 1\")"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01"), "jsonb_path_exists", "$.equal_key ? (@ == \"1970-01-01\")"),
                null
        ).filter(Objects::nonNull);
    }

    static Stream<Arguments> customized() {

        return Stream.of(
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("value"), "my_jsonb_path_exists", "$.equal_key ? (@ == \"value\")"),
                arguments(RSQLOperators.GREATER_THAN, "json.greater_than_key", Collections.singletonList("value"), "my_jsonb_path_exists", "$.greater_than_key ? (@ > \"value\")"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01T00:00:00.000"), "my_jsonb_path_exists", "$.equal_key ? (@.datetime() == \"1970-01-01T00:00:00.000\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01T00:00:00.000Z"), "my_jsonb_path_exists_tz", "$.equal_key ? (@.datetime() == \"1970-01-01T00:00:00.000Z\".datetime())"),
                null
        ).filter(Objects::nonNull);
    }

    static Stream<Arguments> temporal() {

        return Stream.of(
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01"), "jsonb_path_exists", "$.equal_key ? (@.datetime() == \"1970-01-01\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("00:00:00"), "jsonb_path_exists", "$.equal_key ? (@.datetime() == \"00:00:00\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("00:00:00.000"), "jsonb_path_exists", "$.equal_key ? (@.datetime() == \"00:00:00.000\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01T00:00:00"), "jsonb_path_exists", "$.equal_key ? (@.datetime() == \"1970-01-01T00:00:00\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01T00:00:00.000"), "jsonb_path_exists", "$.equal_key ? (@.datetime() == \"1970-01-01T00:00:00.000\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01T00:00:00.000Z"), "jsonb_path_exists_tz", "$.equal_key ? (@.datetime() == \"1970-01-01T00:00:00.000Z\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01T00:00:00+00:00"), "jsonb_path_exists_tz", "$.equal_key ? (@.datetime() == \"1970-01-01T00:00:00+00:00\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01T00:00:00.000+00:00"), "jsonb_path_exists_tz", "$.equal_key ? (@.datetime() == \"1970-01-01T00:00:00.000+00:00\".datetime())"),
                arguments(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1970-01-01T00:00:00.000-00:00"), "jsonb_path_exists_tz", "$.equal_key ? (@.datetime() == \"1970-01-01T00:00:00.000-00:00\".datetime())"),
                null
        ).filter(Objects::nonNull);
    }

    @Test
    void pathForNotEqualIsNotSupported() {
        assertThrows(IllegalArgumentException.class, () -> new JsonbExpressionBuilder(RSQLOperators.NOT_EQUAL, "json.not_equal_key", Collections.singletonList("value")));
    }

    @Test
    void pathForNullIsNotSupported() {
        assertThrows(IllegalArgumentException.class, () -> new JsonbExpressionBuilder(RSQLOperators.IS_NULL, "json.null_key", Collections.singletonList("value")));
    }

    @Test
    void pathForNotLikeIsNotSupported() {
        assertThrows(IllegalArgumentException.class, () -> new JsonbExpressionBuilder(RSQLOperators.NOT_LIKE, "json.not_like_key", Collections.singletonList("value")));
    }

    @Test
    void pathForNotLikeIgnoreCaseIsNotSupported() {
        assertThrows(IllegalArgumentException.class, () -> new JsonbExpressionBuilder(RSQLOperators.IGNORE_CASE_NOT_LIKE, "json.not_like_key", Collections.singletonList("value")));
    }

    @Test
    void pathForNotInIsNotSupported() {
        assertThrows(IllegalArgumentException.class, () -> new JsonbExpressionBuilder(RSQLOperators.NOT_IN, "json.not_in_key", Collections.singletonList("value")));
    }

    @Test
    void pathForNotInIsNotBetween() {
        assertThrows(IllegalArgumentException.class, () -> new JsonbExpressionBuilder(RSQLOperators.NOT_IN, "json.not_in_key", List.of("1", "2")));
    }

}