package io.github.perplexhub.rsql;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class SortUtils {

    private static final Pattern MULTIPLE_SORT_SEPARATOR = Pattern.compile(";");
    private static final Pattern SORT_SEPARATOR = Pattern.compile(",");

    private SortUtils() {
    }

    static List<Order> parseSort(@Nullable final String sort, final Map<String, String> propertyMapper, final Root<?> root, final CriteriaBuilder cb) {
        final SortSupport sortSupport = SortSupport.builder().sortQuery(sort).propertyPathMapper(propertyMapper).build();
        return parseSort(sortSupport, root, cb);
    }

    static List<Order> parseSort(final SortSupport sortSupport, final Root<?> root, final CriteriaBuilder cb) {
        if (!StringUtils.hasText(sortSupport.getSortQuery())) {
            return new ArrayList<>();
        }

        return MULTIPLE_SORT_SEPARATOR.splitAsStream(sortSupport.getSortQuery())
                .map(SortUtils::split)
                .filter(parts -> parts.length > 0)
                .map(parts -> sortToJpaOrder(parts, sortSupport, root, cb))
                .collect(Collectors.toList());
    }

    private static String[] split(String sort) {
        return SORT_SEPARATOR.splitAsStream(sort)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    @SuppressWarnings("unchecked")
    private static Order sortToJpaOrder(final String[] parts, final SortSupport sortSupport, final Root<?> root, final CriteriaBuilder cb) {
        final String property = parts[0];
        final String direction = parts.length > 1 ? parts[1] : "asc";

        final RSQLJPAPredicateConverter rsqljpaPredicateConverter =
                new RSQLJPAPredicateConverter(cb, sortSupport.getPropertyPathMapper(), null, sortSupport.getJoinHints());
        final RSQLJPAContext rsqljpaContext = rsqljpaPredicateConverter.findPropertyPath(property, root);

        Expression<?> propertyExpression = rsqljpaContext.getPath();
        if (parts.length > 2 && "ic".equalsIgnoreCase(parts[2]) && String.class.isAssignableFrom(propertyExpression.getJavaType())) {
            propertyExpression = cb.lower((Expression<String>) propertyExpression);
        }

        return direction.equalsIgnoreCase("asc") ? cb.asc(propertyExpression) : cb.desc(propertyExpression);
    }

}
