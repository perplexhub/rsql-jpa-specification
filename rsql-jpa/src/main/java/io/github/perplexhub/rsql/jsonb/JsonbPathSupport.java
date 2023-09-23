package io.github.perplexhub.rsql.jsonb;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.perplexhub.rsql.*;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class JsonbPathSupport {

    private static final String JSONB_PATH_EXISTS = "jsonb_path_exists";

    private static final Map<ComparisonOperator, ComparisonOperator> NEGATE_OPERATORS =
            Map.of(
                    RSQLOperators.NOT_EQUAL, RSQLOperators.EQUAL,
                    RSQLOperators.NOT_IN, RSQLOperators.IN,
                    RSQLOperators.IS_NULL, RSQLOperators.NOT_NULL,
                    RSQLOperators.NOT_LIKE, RSQLOperators.LIKE,
                    RSQLOperators.IGNORE_CASE_NOT_LIKE, RSQLOperators.IGNORE_CASE_LIKE,
                    RSQLOperators.NOT_BETWEEN, RSQLOperators.BETWEEN
            );

    private static final List<? extends RSQLCustomPredicate<String>> JSONB_CUSTOM_PREDICATES =
            RSQLOperators.supportedOperators().stream()
                    .map(JsonbPathSupport::jsonbPathExistsPredicate)
                    .toList();

    private static RSQLCustomPredicate<String> jsonbPathExistsPredicate(ComparisonOperator operator) {
        Function<RSQLCustomPredicateInput, Predicate> custom = input -> {
            var builder = input.getCriteriaBuilder();
            var attrPath = input.getPath();
            var arguments = input.getArguments().stream().map(Object::toString).toList();
            if(RSQLJPAPredicateConverter.isJsonType(input.getAttribute())) {
                var selector = input.getAttributeName();
                var mayBeInvertedOperator = Optional.ofNullable(NEGATE_OPERATORS.get(operator));
                var jsb = new JsonbExpressionBuilder(mayBeInvertedOperator.orElse(operator), selector, arguments);
                var function = builder.function(JSONB_PATH_EXISTS, Boolean.class, attrPath, builder.literal(jsb.getJsonPathExpression()));
                if (mayBeInvertedOperator.isPresent()) {
                    return builder.isFalse(function);
                } else {
                    return builder.isTrue(function);
                }
            } else {
                RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(builder, Map.of());
                ComparisonNode node = new ComparisonNode(operator, input.getAttributeName(), arguments);
                return converter.visit(node, input.getRoot());
            }
        };
        return new RSQLCustomPredicate<>(operator, String.class, custom);
    }

    public static QuerySupport query(String rsqlKeyPath) {
        return QuerySupport.builder()
                .rsqlQuery(rsqlKeyPath)
                .customPredicates(new ArrayList<>(JSONB_CUSTOM_PREDICATES))
                .build();
    }

}
