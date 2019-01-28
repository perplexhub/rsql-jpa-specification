package io.github.perplexhub.rsql.jpa;

import javax.persistence.EntityManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsqlJpaSpecificationConfig {

	@Bean
	public RsqlJpaSpecification rsqlJpaSpecification(EntityManager entityManager) {
		return new RsqlJpaSpecification(entityManager);
	}

}
