package io.github.perplexhub.rsql;

import lombok.Builder;
import lombok.Data;

import jakarta.persistence.criteria.JoinType;
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

    @Override
    public String toString() {
        return String.format("%s,distinct:%b,propertyPathMapper:%s,customPredicates:%d,joinHints:%s,propertyWhitelist:%s,propertyBlacklist:%s",
                rsqlQuery, distinct, propertyPathMapper, customPredicates == null ? 0 : customPredicates.size(), joinHints, propertyWhitelist, propertyBlacklist);
    }
}
