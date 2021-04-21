package io.github.perplexhub.rsql;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;

import lombok.Value;

@Value(staticConstructor = "of")
public class RSQLCustomPredicateInput {

	private CriteriaBuilder criteriaBuilder;
	private Path<?> path;
	private List<Object> arguments;
	private From root;

}
