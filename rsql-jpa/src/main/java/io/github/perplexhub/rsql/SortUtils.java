package io.github.perplexhub.rsql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.perplexhub.rsql.jsonb.JsonbSupport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.criteria.JpaExpression;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

class SortUtils {

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

    private static Order sortToJpaOrder(final String[] parts, final SortSupport sortSupport, final Root<?> root,
            final CriteriaBuilder cb) {
        final String property = parts[0];

        Selector selector = Selector.selectorOf(property, cb);

        Selector.assertWhiteListed(selector, sortSupport.getProcedureWhiteList());
        Selector.assertNotBlackListed(selector, sortSupport.getProcedureBlackList());

        final String direction = parts.length > 1 ? parts[1] : "asc";

        final RSQLJPAPredicateConverter converter =
                new RSQLJPAPredicateConverter(cb, sortSupport.getPropertyPathMapper(), null,
                        sortSupport.getJoinHints(), sortSupport.getProcedureWhiteList(),
                        sortSupport.getProcedureBlackList());

        final boolean ic = parts.length > 2 && "ic".equalsIgnoreCase(parts[2]);
        Expression<?> propertyExpression = selector.getExpression((string, builder) ->{
            final RSQLJPAContext rsqljpaContext = converter.findPropertyPath(string, root);
            final boolean isJson = JsonbSupport.isJsonType(rsqljpaContext.getAttribute());
            return isJson
                    ? sortExpressionOfJson(rsqljpaContext, string, sortSupport.getPropertyPathMapper(), builder, ic)
                    : rsqljpaContext.getPath();
        });

        if (ic && String.class.isAssignableFrom(propertyExpression.getJavaType())) {
            propertyExpression = cb.lower(propertyExpression.as(String.class));
        }

        return direction.equalsIgnoreCase("asc") ? cb.asc(propertyExpression) : cb.desc(propertyExpression);
    }

    /**
     * Builds a jsonb expression for a given keyPath and operator.<br>
     * If the jsonb expression targets a nested jsonb property, the jsonb expression will be built using the jsonb_extract_path function.
     * Otherwise, the jsonb expression will be built using the jsonb value as a text.
     *
     * @param context  the rsql context
     * @param property the property
     * @param builder  the criteria builder
     * @return the jsonb expression
     */
    private static Expression<?> sortExpressionOfJson(RSQLJPAContext context,
                                                      String property,
                                                      Map<String, String> mapping,
                                                      CriteriaBuilder builder,
                                                      boolean ic) {
        String path = PathUtils.expectBestMapping(property, mapping);
        String jsonbSelector = JsonbSupport.jsonPathOfSelector(context.getAttribute(), path);
        if(jsonbSelector.contains(".")) {
            var args = new ArrayList<Expression<?>>();
            args.add(context.getPath());
            Stream.of(jsonbSelector.split("\\."))
                    .skip(1) // skip root
                    .map(builder::literal)
                    .forEach(args::add);
            Expression<?> expression = builder.function("jsonb_extract_path", Object.class, args.toArray(Expression[]::new));
            if (ic && expression instanceof JpaExpression<?> jpaExpression) {
                expression = jpaExpression.cast(String.class);
            }
            return expression;
        } else {
            return context.getPath().as(String.class);
        }
    }

}
