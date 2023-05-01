package io.github.perplexhub.rsql;

import jakarta.persistence.criteria.JoinType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SortSupport {
    private String sortQuery;
    private Map<String, String> propertyPathMapper;
    private Map<String, JoinType> joinHints;

    @Override
    public String toString() {
        return String.format("%s,propertyPathMapper:%s,joinHints:%s",
                sortQuery, propertyPathMapper, joinHints);
    }
}
