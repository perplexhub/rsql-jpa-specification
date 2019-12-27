package io.github.perplexhub.rsql;

import java.util.Map;

import javax.persistence.EntityManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnClass(EntityManager.class)
public class RSQLJPAAutoConfiguration {

	@Bean
	public RSQLCommonSupport rsqlCommonSupport(Map<String, EntityManager> entityManagerMap) {
		log.info("RSQLJPAAutoConfiguration.rsqlCommonSupport(entityManagerMap:{})", entityManagerMap.size());
		return new RSQLCommonSupport(entityManagerMap);
	}

}
