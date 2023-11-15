package io.github.perplexhub.rsql.jsonb;


import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.perplexhub.rsql.RSQLOperators;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Map;
import java.util.Optional;

/**
 * Support for jsonb expression.
 */
public class JsonbSupport {

    public static boolean DATE_TIME_SUPPORT = false;

    private static final Map<ComparisonOperator, ComparisonOperator> NEGATE_OPERATORS =
            Map.of(
                    RSQLOperators.NOT_EQUAL, RSQLOperators.EQUAL,
                    RSQLOperators.NOT_IN, RSQLOperators.IN,
                    RSQLOperators.IS_NULL, RSQLOperators.NOT_NULL,
                    RSQLOperators.NOT_LIKE, RSQLOperators.LIKE,
                    RSQLOperators.IGNORE_CASE_NOT_LIKE, RSQLOperators.IGNORE_CASE_LIKE,
                    RSQLOperators.NOT_BETWEEN, RSQLOperators.BETWEEN
            );

    record JsonbPathExpression(String jsonbFunction, String jsonbPath) {
    }

    /**
     * Builds a jsonb expression for a given keyPath and operator.
     *
     * @param builder  the criteria builder
     * @param node     the comparison node
     * @param attrPath the attribute jsonbPath
     * @return the jsonb expression
     */
    public static Predicate jsonbPathExists(CriteriaBuilder builder, ComparisonNode node, Path<?> attrPath) {
        var mayBeInvertedOperator = Optional.ofNullable(NEGATE_OPERATORS.get(node.getOperator()));
        var jsb = new JsonbExpressionBuilder(mayBeInvertedOperator.orElse(node.getOperator()), node.getSelector(), node.getArguments());
        var expression = jsb.getJsonPathExpression();
        var function = builder.function(expression.jsonbFunction, Boolean.class, attrPath,
                builder.literal(expression.jsonbPath));
        if (mayBeInvertedOperator.isPresent()) {
            return builder.isFalse(function);
        } else {
            return builder.isTrue(function);
        }
    }
}
