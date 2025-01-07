package io.github.perplexhub.rsql.jsonb;


import static io.github.perplexhub.rsql.RSQLVisitorBase.getEntityManagerMap;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.perplexhub.rsql.RSQLOperators;
import io.github.perplexhub.rsql.RSQLVisitorBase;
import io.github.perplexhub.rsql.ResolvedExpression;
import jakarta.persistence.Column;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import org.springframework.orm.jpa.vendor.Database;

/**
 * Support for jsonb expression.
 */
public class JsonbSupport {

    public static boolean DATE_TIME_SUPPORT = false;

    private static final Set<Database> JSON_SUPPORT = EnumSet.of(Database.POSTGRESQL);

    private static final Map<ComparisonOperator, ComparisonOperator> NEGATE_OPERATORS =
            Map.of(
                    RSQLOperators.NOT_EQUAL, RSQLOperators.EQUAL,
                    RSQLOperators.NOT_IN, RSQLOperators.IN,
                    RSQLOperators.IS_NULL, RSQLOperators.NOT_NULL,
                    RSQLOperators.NOT_LIKE, RSQLOperators.LIKE,
                    RSQLOperators.IGNORE_CASE_NOT_LIKE, RSQLOperators.IGNORE_CASE_LIKE,
                    RSQLOperators.NOT_BETWEEN, RSQLOperators.BETWEEN
            );

    /**
     * Returns the jsonb path for the given attribute path and selector.<br>
     * It extracts the jsonb part of the selector that can contains entity references before the jsonb path.
     *
     * @param attrPath the attribute path
     * @param selector the selector
     * @return the jsonb path
     */
    public static String jsonPathOfSelector(Attribute<?, ?> attrPath, String selector) {
        String attributeName = attrPath.getName();
        int attributePosition = selector.indexOf(attributeName);
        if(attributePosition < 0) {
            throw new IllegalArgumentException("The attribute name [" + attributeName + "] is not part of the selector [" + selector + "]");
        }
        return selector.substring(attributePosition + attributeName.length());
    }

    record JsonbPathExpression(String jsonbFunction, String jsonbPath) {
    }


    public static ResolvedExpression jsonbPathExistsExpression(CriteriaBuilder builder, ComparisonNode node, Path<?> attrPath) {
        var mayBeInvertedOperator = Optional.ofNullable(NEGATE_OPERATORS.get(node.getOperator()));
        var jsb = new JsonbExpressionBuilder(mayBeInvertedOperator.orElse(node.getOperator()), node.getSelector(), node.getArguments());
        var expression = jsb.getJsonPathExpression();
        return ResolvedExpression.ofJson(builder.function(expression.jsonbFunction, Boolean.class, attrPath,
                builder.literal(expression.jsonbPath)), mayBeInvertedOperator.isPresent());
    }

    /**
     * Returns whether the given attribute is a jsonb attribute.
     *
     * @param attribute the attribute
     * @return true if the attribute is a jsonb attribute
     */
    public static boolean isJsonType(Attribute<?, ?> attribute) {
        return attribute!=null
                && isJsonColumn(attribute)
                && getDatabase(attribute).map(JSON_SUPPORT::contains).orElse(false);
    }

    /**
     * Returns whether the given property is a jsonb attribute.
     *
     * @param mappedProperty the mapped property
     * @param classMetadata  the class metadata
     * @return true if the attribute is a jsonb attribute
     */
    public static boolean isJsonType(String mappedProperty, ManagedType<?> classMetadata) {
        return Optional.ofNullable(RSQLVisitorBase.getAttribute(mappedProperty, classMetadata))
                .map(JsonbSupport::isJsonType)
                .orElse(false);
    }

    /**
     * Returns whether the given attribute is a jsonb attribute.
     *
     * @param attribute the attribute
     * @return true if the attribute is a jsonb attribute
     */
    private static boolean isJsonColumn(Attribute<?, ?> attribute) {
        return Optional.ofNullable(attribute)
                .filter(attr -> attr.getJavaMember() instanceof Field)
                .map(attr -> ((Field) attr.getJavaMember()))
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::columnDefinition)
                .map("jsonb"::equalsIgnoreCase)
                .orElse(false);
    }

    /**
     * Returns the database of the given attribute.
     *
     * @param attribute the attribute
     * @return the database
     */
    private static Optional<Database> getDatabase(Attribute<?, ?> attribute) {
        return getEntityManagerMap()
                .values()
                .stream()
                .filter(em -> em.getMetamodel().getManagedTypes().contains(attribute.getDeclaringType()))
                .findFirst()
                .map(RSQLVisitorBase::getDatabase);
    }
}
