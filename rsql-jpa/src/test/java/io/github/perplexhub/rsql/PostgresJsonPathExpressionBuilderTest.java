package io.github.perplexhub.rsql;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostgresJsonPathExpressionBuilderTest {

    @Test
    void testLongKeyPath() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.data.other.long_key_path", Collections.singletonList("value"));
        assertEquals("$.data.other.long_key_path ? (@ == \"value\")", builder.getJsonPathTest());
    }

    @Test
    void pathForNotNullComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.NOT_NULL, "json.not_null_key", Collections.emptyList());
        assertEquals("$.not_null_key ? (@ != null)", builder.getJsonPathTest());
    }

    @Test
    void pathForEqualComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("value"));
        assertEquals("$.equal_key ? (@ == \"value\")", builder.getJsonPathTest());
    }

    @Test
    void pathForGreaterThanComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.GREATER_THAN, "json.greater_than_key", Collections.singletonList("value"));
        assertEquals("$.greater_than_key ? (@ > \"value\")", builder.getJsonPathTest());
    }

    @Test
    void pathForGreaterThanOrEqualComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.GREATER_THAN_OR_EQUAL, "json.greater_than_or_equal_key", Collections.singletonList("value"));
        assertEquals("$.greater_than_or_equal_key ? (@ >= \"value\")", builder.getJsonPathTest());
    }

    @Test
    void pathForLessThanComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.LESS_THAN, "json.less_than_key", Collections.singletonList("value"));
        assertEquals("$.less_than_key ? (@ < \"value\")", builder.getJsonPathTest());
    }

    @Test
    void pathForLessThanOrEqualComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.LESS_THAN_OR_EQUAL, "json.less_than_or_equal_key", Collections.singletonList("value"));
        assertEquals("$.less_than_or_equal_key ? (@ <= \"value\")", builder.getJsonPathTest());
    }

    @Test
    void pathForBetweenComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.BETWEEN, "json.between_key", List.of("1", "2"));
        assertEquals("$.between_key ? (@ >= 1 && @ <= 2)", builder.getJsonPathTest());
    }

    @Test
    void pathForEqualWithIntegerAsValueComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1"));
        assertEquals("$.equal_key ? (@ == 1)", builder.getJsonPathTest());
    }

    @Test
    void pathForEqualWithBooleanAsValueComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("true"));
        assertEquals("$.equal_key ? (@ == true)", builder.getJsonPathTest());
    }

    @Test
    void pathForEqualWithDoubleAsValueComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1.0"));
        assertEquals("$.equal_key ? (@ == 1.0)", builder.getJsonPathTest());
    }

    @Test
    void pathForEqualWithTimeAsValueComparison() {
        PostgresJsonPathExpressionBuilder.dateTimeSupport = true;
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13\".datetime())", builder.getJsonPathTest());
    }

    @Test
    void pathForEqualWithLegacyTimeAsValueComparison() {
        PostgresJsonPathExpressionBuilder.dateTimeSupport = false;
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13"));
        assertEquals("$.equal_key ? (@ == \"10:12:13\")", builder.getJsonPathTest());
    }


}