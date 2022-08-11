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
    private Map<String, String> propertyPathMapper;
    private List<RSQLCustomPredicate<?>> customPredicates;
    private Map<String, JoinType> joinHints;
    private Map<Class<?>, List<String>> propertyWhitelist;
    private Map<Class<?>, List<String>> propertyBlacklist;
}
