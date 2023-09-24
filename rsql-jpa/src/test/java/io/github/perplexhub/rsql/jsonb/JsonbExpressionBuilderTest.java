package io.github.perplexhub.rsql.jsonb;

import io.github.perplexhub.rsql.RSQLOperators;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonbExpressionBuilderTest {
    @Test
    void testLongKeyPath() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.data.other.long_key_path", Collections.singletonList("value"));
        assertEquals("$.data.other.long_key_path ? (@ == \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("value"));
        assertEquals("$.equal_key ? (@ == \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForGreaterThanComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.GREATER_THAN, "json.greater_than_key", Collections.singletonList("value"));
        assertEquals("$.greater_than_key ? (@ > \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForGreaterThanOrEqualComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.GREATER_THAN_OR_EQUAL, "json.greater_than_or_equal_key", Collections.singletonList("value"));
        assertEquals("$.greater_than_or_equal_key ? (@ >= \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLessThanComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.LESS_THAN, "json.less_than_key", Collections.singletonList("value"));
        assertEquals("$.less_than_key ? (@ < \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLessThanOrEqualComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.LESS_THAN_OR_EQUAL, "json.less_than_or_equal_key", Collections.singletonList("value"));
        assertEquals("$.less_than_or_equal_key ? (@ <= \"value\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForBetweenComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.BETWEEN, "json.between_key", List.of("1", "2"));
        assertEquals("$.between_key ? (@ >= 1 && @ <= 2)", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithIntegerAsValueComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1"));
        assertEquals("$.equal_key ? (@ == 1)", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithBooleanAsValueComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("true"));
        assertEquals("$.equal_key ? (@ == true)", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithDoubleAsValueComparison() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("1.0"));
        assertEquals("$.equal_key ? (@ == 1.0)", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithSimpleTimeAsValueComparison() {
        JsonbPathSupport.dateTimeSupport = true;
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13\".datetime())", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithTimeAndTimeZoneAsValueComparison() {
        JsonbPathSupport.dateTimeSupport = true;
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13+01:00"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13+01:00\".datetime())", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithTimeWithMillisAsValueComparison() {
        JsonbPathSupport.dateTimeSupport = true;
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13.123"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13.123\".datetime())", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithTimeWithMillisAndTimeZoneAsValueComparison() {
        JsonbPathSupport.dateTimeSupport = true;
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13.123+01:00"));
        assertEquals("$.equal_key ? (@.datetime() == \"10:12:13.123+01:00\".datetime())", builder.getJsonPathExpression());
    }

    @Test
    void pathForEqualWithLegacyTimeAsValueComparison() {
        JsonbPathSupport.dateTimeSupport = false;
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.EQUAL, "json.equal_key", Collections.singletonList("10:12:13"));
        assertEquals("$.equal_key ? (@ == \"10:12:13\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLikeWithBooleanAsArgumentWillCompareString() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.LIKE, "json.like_key", Collections.singletonList("true"));
        assertEquals("$.like_key ? (@ like_regex \".*true.*\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLikeWithIntegerAsArgumentWillCompareString() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.LIKE, "json.like_key", Collections.singletonList("1"));
        assertEquals("$.like_key ? (@ like_regex \".*1.*\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLikeWithTimeAsArgumentWillCompareString() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.LIKE, "json.like_key", Collections.singletonList("10:12:13"));
        assertEquals("$.like_key ? (@ like_regex \".*10:12:13.*\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLikeWithDateAsArgumentWillCompareString() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.LIKE, "json.like_key", Collections.singletonList("2020-01-01"));
        assertEquals("$.like_key ? (@ like_regex \".*2020-01-01.*\")", builder.getJsonPathExpression());
    }

    @Test
    void pathForLikeWithDateTimeAsArgumentWillCompareString() {
        JsonbExpressionBuilder builder = new JsonbExpressionBuilder(RSQLOperators.LIKE, "json.like_key", Collections.singletonList("2020-01-01T10:12:13"));
        assertEquals("$.like_key ? (@ like_regex \".*2020-01-01T10:12:13.*\")", builder.getJsonPathExpression());
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