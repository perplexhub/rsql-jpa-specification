package io.github.perplexhub.rsql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import org.springframework.lang.Nullable;

class SortUtils {

    private static final String MULTIPLE_SORT_SEPARATOR = ";";
    private static final String SORT_SEPARATOR = ",";

    static List<Order> parseSort(@Nullable final String sort, final Map<String, String> propertyMapper, final Root<?> root, final CriteriaBuilder cb) {
        if (sort == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(sort.split(MULTIPLE_SORT_SEPARATOR))
            .map(item -> item.split(SORT_SEPARATOR))
            .map(parts -> sortToJpaOrder(parts, propertyMapper, root, cb))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static Order sortToJpaOrder(final String[] parts, final Map<String, String> propertyMapper, final Root<?> root, final CriteriaBuilder cb) {
        final String property = parts[0];
        final String direction = parts.length > 1 ? parts[1] : "asc";

        final RSQLJPAPredicateConverter rsqljpaPredicateConverter = new RSQLJPAPredicateConverter(cb, propertyMapper);
        final RSQLJPAContext rsqljpaContext = rsqljpaPredicateConverter.findPropertyPath(property, root);

        Expression<?> propertyExpression = rsqljpaContext.getPath();
        if (parts.length > 2 && "ic".equalsIgnoreCase(parts[2]) && String.class.isAssignableFrom(propertyExpression.getJavaType())) {
            propertyExpression = cb.lower((Expression<String>) propertyExpression);
        }

        return direction.equalsIgnoreCase("asc") ? cb.asc(propertyExpression) : cb.desc(propertyExpression);
    }

}
