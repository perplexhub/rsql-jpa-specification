package io.github.perplexhub.rsql;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostgresJsonPathExpressionBuilderTest {

    @Test
    void testLongKeyPath() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.data.other.long_key_path", Collections.singletonList("value"));
        assertEquals("$.data.other.long_key_path ? (@ == \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("value"));
        assertEquals("$.equal_key ? (@ == \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForGreaterThanComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.GREATER_THAN, "json.greater_than_key", Collections.singletonList("value"));
        assertEquals("$.greater_than_key ? (@ > \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForGreaterThanOrEqualComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.GREATER_THAN_OR_EQUAL, "json.greater_than_or_equal_key", Collections.singletonList("value"));
        assertEquals("$.greater_than_or_equal_key ? (@ >= \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLessThanComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.LESS_THAN, "json.less_than_key", Collections.singletonList("value"));
        assertEquals("$.less_than_key ? (@ < \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLessThanOrEqualComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.LESS_THAN_OR_EQUAL, "json.less_than_or_equal_key", Collections.singletonList("value"));
        assertEquals("$.less_than_or_equal_key ? (@ <= \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForBetweenComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.BETWEEN, "json.between_key", List.of("1", "2"));
        assertEquals("$.between_key ? (@ >= 1 && @ <= 2)", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithIntegerAsValueComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1"));
        assertEquals("$.equal_key ? (@ == 1)", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithBooleanAsValueComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("true"));
        assertEquals("$.equal_key ? (@ == true)", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithDoubleAsValueComparison() {
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1.0"));
        assertEquals("$.equal_key ? (@ == 1.0)", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithSimpleTimeAsValueComparison() {
        PostgresJsonPathExpressionBuilder.dateTimeSupport = true;
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13\".datetime())", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithTimeAndTimeZoneAsValueComparison() {
        PostgresJsonPathExpressionBuilder.dateTimeSupport = true;
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13+01:00"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13+01:00\".datetime())", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithTimeWithMillisAsValueComparison() {
        PostgresJsonPathExpressionBuilder.dateTimeSupport = true;
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13.123"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13.123\".datetime())", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithTimeWithMillisAndTimeZoneAsValueComparison() {
        PostgresJsonPathExpressionBuilder.dateTimeSupport = true;
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13.123+01:00"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13.123+01:00\".datetime())", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithLegacyTimeAsValueComparison() {
        PostgresJsonPathExpressionBuilder.dateTimeSupport = false;
        PostgresJsonPathExpressionBuilder builder = new PostgresJsonPathExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13"));
        assertEquals("$.equal_key ? (@ == \"10:12:13\")", builder.getJsonPathExpression());
    }


}