package io.github.perplexhub.rsql;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Builds a predicate for a JSON that use the Postgres JSONB functions jsonb_path_exists and jsonb_path_exists.
 */
public class PostgresJsonPredicateBuilder {
    private final CriteriaBuilder builder;
    private boolean invertPredicate = false;

    /**
     * Creates a new instance.
     * @param builder the criteria builder
     */
    public PostgresJsonPredicateBuilder(CriteriaBuilder builder) {
        this.builder = builder;
    }

    /**
     * Builds a predicate for a JSON that use the Postgres JSONB functions jsonb_path_exists and jsonb_path_exists.
     * @param comparisonNode the comparison node
     * @param attrPath the path to the JSON attribute
     * @return the predicate
     */
    Predicate build(ComparisonNode comparisonNode, Path<?> attrPath) {
        var jsb = new PostgresJsonPathExpressionBuilder(convertToAlwaysTrueOperator(comparisonNode.getOperator()),
                comparisonNode.getSelector(), comparisonNode.getArguments());
        var function = builder.function("jsonb_path_exists", Boolean.class, attrPath,
                builder.literal(jsb.getJsonPathExpression()));
        if (invertPredicate) {
            return builder.isFalse(function);
        } else {
            return builder.isTrue(function);
        }
    }

    /**
     * Converts operators that are not supported by jsonb_path_exists to operators that are always true.
     * @param operator the operator to convert
     * @return the converted operator
     */
    private ComparisonOperator convertToAlwaysTrueOperator(ComparisonOperator operator) {
        if (RSQLOperators.NOT_EQUAL.equals(operator)) {
            invertPredicate = true;
            return RSQLOperators.EQUAL;
        }
        if (RSQLOperators.IS_NULL.equals(operator)) {
            invertPredicate = true;
            return RSQLOperators.NOT_NULL;
        }
        if (RSQLOperators.NOT_IN.equals(operator)) {
            invertPredicate = true;
            return RSQLOperators.IN;
        }
        if (RSQLOperators.NOT_LIKE.equals(operator)) {
            invertPredicate = true;
            return RSQLOperators.LIKE;
        }
        if (RSQLOperators.IGNORE_CASE_NOT_LIKE.equals(operator)) {
            invertPredicate = true;
            return RSQLOperators.IGNORE_CASE_LIKE;
        }
        if (RSQLOperators.NOT_BETWEEN.equals(operator)) {
            invertPredicate = true;
            return RSQLOperators.BETWEEN;
        }
        return operator;
    }
}
