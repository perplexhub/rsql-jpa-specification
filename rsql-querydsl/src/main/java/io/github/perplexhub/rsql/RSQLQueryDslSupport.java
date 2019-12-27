package io.github.perplexhub.rsql;

import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;

import org.springframework.util.StringUtils;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;

import cz.jirutka.rsql.parser.RSQLParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes" })
public class RSQLQueryDslSupport extends RSQLJPASupport {

	public RSQLQueryDslSupport() {
		super();
	}

	public RSQLQueryDslSupport(Map<String, EntityManager> entityManagerMap) {
		super(entityManagerMap);
	}

	public static BooleanExpression toPredicate(final String rsqlQuery, final Path qClazz) {
		return toPredicate(rsqlQuery, qClazz, null);
	}

	public static BooleanExpression toPredicate(final String rsqlQuery, final Path qClazz, final Map<String, String> propertyPathMapper) {
		log.debug("toPredicate({},qClazz:{},propertyPathMapper:{})", rsqlQuery, qClazz);
		if (StringUtils.hasText(rsqlQuery)) {
			return new RSQLParser(RSQLOperators.supportedOperators())
					.parse(rsqlQuery)
					.accept(new RSQLQueryDslPredicateConverter(propertyPathMapper), qClazz);
		} else {
			return null;
		}
	}

	protected String getVersion() {
		try {
			Properties prop = new Properties();
			prop.load(getClass().getResourceAsStream("/META-INF/maven/io.github.perplexhub/rsql-querydsl/pom.properties"));
			String version = prop.getProperty("version");
			return StringUtils.hasText(version) ? "[" + version + "] " : "";
		} catch (Exception e) {
			return "";
		}
	}

}
