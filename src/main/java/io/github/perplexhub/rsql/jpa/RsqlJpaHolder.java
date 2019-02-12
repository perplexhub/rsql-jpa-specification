package io.github.perplexhub.rsql.jpa;

import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;

class RsqlJpaHolder<X, Y> {
	private Path<X> path;
	private Attribute<X, Y> attribute;

	public Path<X> getPath() {
		return path;
	}

	public void setPath(Path<X> path) {
		this.path = path;
	}

	public Attribute<X, Y> getAttribute() {
		return attribute;
	}

	public void setAttribute(Attribute<X, Y> attribute) {
		this.attribute = attribute;
	}
}