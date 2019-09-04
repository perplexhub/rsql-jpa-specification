package io.github.perplexhub.rsql;

import javax.persistence.metamodel.Attribute;

import lombok.Value;

@Value(staticConstructor = "of")
class RSQLQueryDslContext {

	private String propertyPath;
	private Attribute<?, ?> attribute;

}
