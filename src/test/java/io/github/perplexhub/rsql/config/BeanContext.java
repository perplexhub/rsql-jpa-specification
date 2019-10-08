package io.github.perplexhub.rsql.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Import(io.github.perplexhub.rsql.RSQLConfig.class)
@EnableJpaRepositories(basePackages = { "io.github.perplexhub.rsql.repository" })
@EnableTransactionManagement
public class BeanContext {

}
