package io.github.perplexhub.rsql;

import javax.persistence.metamodel.Attribute;

import com.querydsl.core.types.Path;

import lombok.Value;

@Value(staticConstructor = "of")
class RSQLQueryDslContext {

	private String propertyPath;
	private Attribute<?, ?> attribute;
	private Path<?> entityClass;

}
