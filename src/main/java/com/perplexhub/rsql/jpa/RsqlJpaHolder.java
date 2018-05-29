package com.perplexhub.rsql.jpa;

import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;

class RsqlJpaHolder<X, Y> {
	Path<X> path;
	Attribute<X, Y> attribute;
}