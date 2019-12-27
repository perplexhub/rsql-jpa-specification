package io.github.perplexhub.rsql;

import javax.persistence.EntityManager;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RSQLConfig {

	@Bean
	public RSQLSupport rsqlSupport(ApplicationContext applicationContext) {
		return new RSQLSupport(applicationContext.getBeansOfType(EntityManager.class));
	}

}
