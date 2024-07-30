package io.github.perplexhub.rsql;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Attribute;

import jakarta.persistence.metamodel.ManagedType;
import lombok.Value;

@Value(staticConstructor = "of")
class RSQLJPAContext {

	Path<?> path;
	Attribute<?, ?> attribute;
	ManagedType<?> managedType;

}
