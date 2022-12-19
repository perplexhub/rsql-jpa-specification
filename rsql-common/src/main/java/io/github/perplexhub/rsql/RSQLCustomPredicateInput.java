package io.github.perplexhub.rsql;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Attribute;

import lombok.Value;

@Value(staticConstructor = "of")
public class RSQLCustomPredicateInput {

    CriteriaBuilder criteriaBuilder;
    Path<?> path;
    Attribute<?, ?> attribute;
    List<Object> arguments;
    From<?, ?> root;

    public static RSQLCustomPredicateInput of(
            CriteriaBuilder criteriaBuilder,
            Path<?> path,
            List<Object> arguments,
            From<?, ?> root
    ) {
        return of(criteriaBuilder, path, null, arguments, root);
    }

}
