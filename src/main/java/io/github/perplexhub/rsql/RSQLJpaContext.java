package io.github.perplexhub.rsql;

import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;

import lombok.Value;

@Value(staticConstructor = "of")
class RSQLJpaContext {

	private Path<?> path;
	private Attribute<?, ?> attribute;

}
