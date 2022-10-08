package io.github.perplexhub.rsql;

import lombok.Builder;
import lombok.Data;

import javax.persistence.criteria.JoinType;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class QuerySupport {
    private String rsqlQuery;
    private boolean distinct;
    /**
     * Whether try to interpret {@link RSQLOperators#EQUAL} or {@link RSQLOperators#NOT_EQUAL} operators as
     * {@link RSQLOperators#LIKE}, {@link RSQLOperators#NOT_LIKE} or their case-insensitive variants.
     */
    private boolean strictEquality;
    private Map<String, String> propertyPathMapper;
    private List<RSQLCustomPredicate<?>> customPredicates;
    private Map<String, JoinType> joinHints;
    private Map<Class<?>, List<String>> propertyWhitelist;
    private Map<Class<?>, List<String>> propertyBlacklist;
}
