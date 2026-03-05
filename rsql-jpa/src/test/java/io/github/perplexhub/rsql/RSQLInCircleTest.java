package io.github.perplexhub.rsql;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.Node;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the IN_CIRCLE spatial operator (=incircle=).
 *
 * <p>
 * These tests validate that:
 * <ul>
 * <li>The operator is registered in
 * {@link RSQLOperators#supportedOperators()}</li>
 * <li>The RSQL parser accepts =incircle= with 3 arguments</li>
 * <li>The predicate builder calls ST_Distance and wraps it with
 * lessThanOrEqualTo</li>
 * </ul>
 *
 * NOTE: Actual database execution is not tested here because H2 does not
 * support
 * ST_Distance. Integration tests against a PostGIS database would be needed for
 * end-to-end validation.
 */
class RSQLInCircleTest {

    @Test
    void inCircleOperatorIsRegistered() {
        Set<cz.jirutka.rsql.parser.ast.ComparisonOperator> ops = RSQLOperators.supportedOperators();
        boolean found = ops.stream()
                .anyMatch(op -> Arrays.asList(op.getSymbols()).contains("=incircle="));
        assertTrue(found, "=incircle= should be in RSQLOperators.supportedOperators()");
    }

    @Test
    void inCircleOperatorHasNary3Arity() {
        // nary(3) means exactly 3 arguments required
        assertTrue(RSQLOperators.IN_CIRCLE.isMultiValue(),
                "IN_CIRCLE must accept multiple values");
        // The operator should be parseable with exactly 3 arguments
        String rsql = "location=incircle=(4.123,-74.456,500)";
        RSQLParser parser = new RSQLParser(RSQLOperators.supportedOperators());
        Node node = assertDoesNotThrow(() -> parser.parse(rsql),
                "Parser should accept =incircle= with 3 arguments");
        assertNotNull(node);
    }

    @Test
    void inCircleNodeHasThreeArguments() {
        String rsql = "location=incircle=(4.123,-74.456,500)";
        RSQLParser parser = new RSQLParser(RSQLOperators.supportedOperators());
        Node node = parser.parse(rsql);

        ComparisonNode compNode = (ComparisonNode) node;
        assertEquals("location", compNode.getSelector());
        assertEquals("=incircle=", compNode.getOperator().getSymbols()[0]);
        List<String> args = compNode.getArguments();
        assertEquals(3, args.size(), "IN_CIRCLE must have exactly 3 arguments");
        assertEquals("4.123", args.get(0));
        assertEquals("-74.456", args.get(1));
        assertEquals("500", args.get(2));
    }

    @Test
    void inCirclePredicateCallsST_Distance() {
        // Verify that the predicate builder calls builder.function("ST_Distance", ...)
        // and wraps in lessThanOrEqualTo using mock CriteriaBuilder
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Expression<Double> mockDistanceExpr = mock(Expression.class);
        @SuppressWarnings("unchecked")
        Predicate mockPredicate = mock(Predicate.class);
        @SuppressWarnings("unchecked")
        Expression<?> mockPath = mock(Expression.class);

        when(cb.function(eq("ST_Distance"), eq(Double.class), any(), any()))
                .thenReturn(mockDistanceExpr);
        when(cb.lessThanOrEqualTo(eq(mockDistanceExpr), eq(500.0)))
                .thenReturn(mockPredicate);

        // Call the converter logic indirectly via RSQLJPAPredicateConverter
        // We verify that builder.function("ST_Distance") is invoked
        RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(cb, null);

        List<String> arguments = List.of("4.123", "-74.456", "500");
        // Use reflection to call the private method
        try {
            var method = RSQLJPAPredicateConverter.class.getDeclaredMethod(
                    "inCirclePredicate", Expression.class, List.class);
            method.setAccessible(true);
            Predicate result = (Predicate) method.invoke(converter, mockPath, arguments);

            // Verify ST_Distance was called with the mock path and a Point literal
            ArgumentCaptor<Object> literalCaptor = ArgumentCaptor.forClass(Object.class);
            verify(cb).function(eq("ST_Distance"), eq(Double.class), eq(mockPath), any());
            verify(cb).lessThanOrEqualTo(eq(mockDistanceExpr), eq(500.0));
            assertEquals(mockPredicate, result);
        } catch (Exception e) {
            fail("Failed to invoke inCirclePredicate: " + e.getMessage());
        }
    }

    @Test
    void inCirclePredicateCreatesCorrectPoint() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Expression<Double> mockDistanceExpr = mock(Expression.class);
        @SuppressWarnings("unchecked")
        Predicate mockPredicate = mock(Predicate.class);
        @SuppressWarnings("unchecked")
        Expression<?> mockPath = mock(Expression.class);

        // Capture the literal passed to cb.function to verify it's a JTS Point
        ArgumentCaptor<Object> literalCaptor = ArgumentCaptor.forClass(Object.class);
        when(cb.literal(literalCaptor.capture())).thenReturn(mock(Expression.class));
        when(cb.function(eq("ST_Distance"), eq(Double.class), any(), any()))
                .thenReturn(mockDistanceExpr);
        when(cb.lessThanOrEqualTo(eq(mockDistanceExpr), anyDouble()))
                .thenReturn(mockPredicate);

        RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(cb, null);
        List<String> arguments = List.of("4.123", "-74.456", "500");

        try {
            var method = RSQLJPAPredicateConverter.class.getDeclaredMethod(
                    "inCirclePredicate", Expression.class, List.class);
            method.setAccessible(true);
            method.invoke(converter, mockPath, arguments);

            // The literal captured should be a JTS Point
            Object capturedLiteral = literalCaptor.getValue();
            assertInstanceOf(Point.class, capturedLiteral,
                    "cb.literal() should receive a JTS Point");

            Point capturedPoint = (Point) capturedLiteral;
            assertEquals(4326, capturedPoint.getSRID(), "Point SRID must be 4326 (WGS84)");
            // Note: JTS uses (lon, lat) = (x, y)
            assertEquals(-74.456, capturedPoint.getX(), 1e-9, "Longitude → X");
            assertEquals(4.123, capturedPoint.getY(), 1e-9, "Latitude → Y");
        } catch (Exception e) {
            fail("Failed to invoke inCirclePredicate: " + e.getMessage());
        }
    }

    @Test
    void inCircleThrowsOnInvalidCoordinates() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(cb, null);
        @SuppressWarnings("unchecked")
        Expression<?> mockPath = mock(Expression.class);

        List<String> badArguments = List.of("not-a-number", "-74.456", "500");

        try {
            var method = RSQLJPAPredicateConverter.class.getDeclaredMethod(
                    "inCirclePredicate", Expression.class, List.class);
            method.setAccessible(true);

            Exception ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> method.invoke(converter, mockPath, badArguments));
            assertInstanceOf(IllegalArgumentException.class, ex.getCause(),
                    "Should throw IllegalArgumentException for invalid coordinate");
        } catch (NoSuchMethodException e) {
            fail("inCirclePredicate method not found: " + e.getMessage());
        }
    }
}
