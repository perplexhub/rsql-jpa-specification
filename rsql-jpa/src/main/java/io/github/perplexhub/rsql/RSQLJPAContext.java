package io.github.perplexhub.rsql;

import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import lombok.Value;

@Value(staticConstructor = "of")
class RSQLJPAContext {

	Path<?> path;
	Attribute<?, ?> attribute;
	ManagedType<?> managedType;

}
