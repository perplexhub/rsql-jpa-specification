package io.github.perplexhub.rsql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.springframework.lang.Nullable;

class SortUtils {

    private static final String MULTIPLE_SORT_SEPARATOR = ";";
    private static final String SORT_SEPARATOR = ",";
    private static final String PROPERTY_PATH_SEPARATOR = "\\.";

    static List<Order> parseSort(@Nullable final String sort, final Map<String, String> propertyMapper, final Root<?> root, final CriteriaBuilder cb) {
        if (sort == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(sort.split(MULTIPLE_SORT_SEPARATOR))
            .map(item -> item.split(SORT_SEPARATOR))
            .map(parts -> sortToJpaOrder(parts, propertyMapper, root, cb))
            .collect(Collectors.toList());
    }

    private static Order sortToJpaOrder(final String[] parts, final Map<String, String> propertyMapper, final Root<?> root, final CriteriaBuilder cb) {
        final String property = parts[0];
        final String direction = parts[1];

        final String propertyPath = propertyMapper.getOrDefault(property, property);
        Expression<?> propertyExpression = pathToExpression(root, propertyPath);
        if (parts.length > 2 && "ic".equalsIgnoreCase(parts[2]) && String.class.isAssignableFrom(propertyExpression.getJavaType())) {
            propertyExpression = cb.lower((Expression<String>) propertyExpression);
        }
        return direction.equalsIgnoreCase("asc") ? cb.asc(propertyExpression) : cb.desc(propertyExpression);
    }

    private static Expression<?> pathToExpression(final Root<?> root, final String path) {
        final String[] properties = path.split(PROPERTY_PATH_SEPARATOR);

        Path<?> expression = root.get(properties[0]);
        for (int i = 1; i < properties.length; ++i) {
            expression = expression.get(properties[i]);
        }
        return expression;
    }

}
