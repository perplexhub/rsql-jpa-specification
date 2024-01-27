package io.github.perplexhub.rsql;

import jakarta.persistence.criteria.Expression;

/**
 * Holder for resolved expression.
 */
public sealed interface ResolvedExpression {

    /**
     * Holder for a path expression.
     */
    record PathExpression(Expression<?> expression, Class<?> type) implements ResolvedExpression {
        public PathExpression {
            if (expression == null) {
                throw new IllegalArgumentException("Expression cannot be null");
            }
            if (type == null) {
                throw new IllegalArgumentException("Type cannot be null");
            }
        }
    }

    /**
     * Holder for a jsonb expression.
     */
    record JsonbPathExpression(Expression<Boolean> expression, boolean inverted) implements ResolvedExpression {
        public JsonbPathExpression {
            if (expression == null) {
                throw new IllegalArgumentException("Expression cannot be null");
            }
        }
    }

    static ResolvedExpression ofPath(Expression<?> expression, Class<?> type) {
        return new PathExpression(expression, type);
    }


    static ResolvedExpression ofJson(Expression<Boolean> expression, boolean inverted) {
        return new JsonbPathExpression(expression, inverted);
    }
}
