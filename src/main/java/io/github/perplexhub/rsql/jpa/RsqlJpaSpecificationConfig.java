package io.github.perplexhub.rsql.jpa;

import javax.persistence.EntityManager;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RsqlJpaSpecificationConfig {

	public RsqlJpaSpecification rsqlJpaSpecification(EntityManager entityManager) {
		return new RsqlJpaSpecification(entityManager);
	}

}
