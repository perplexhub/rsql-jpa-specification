package io.github.perplexhub.rsql;

import java.util.Map;

import javax.persistence.EntityManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = { "io.github.perplexhub.rsql.repository.querydsl" })
@EnableTransactionManagement
@SpringBootApplication
public class Application {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public RSQLCommonSupport rsqlCommonSupport(Map<String, EntityManager> entityManagerMap) {
		return new RSQLCommonSupport(entityManagerMap);
	}

}
