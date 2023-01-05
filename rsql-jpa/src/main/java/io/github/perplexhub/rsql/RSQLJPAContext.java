package io.github.perplexhub.rsql;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Attribute;

import lombok.Value;

@Value(staticConstructor = "of")
class RSQLJPAContext {

	private Path<?> path;
	private Attribute<?, ?> attribute;

}
