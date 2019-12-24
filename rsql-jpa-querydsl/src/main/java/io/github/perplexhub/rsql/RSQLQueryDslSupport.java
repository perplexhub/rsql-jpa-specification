package io.github.perplexhub.rsql;

import java.util.Map;
import java.util.Properties;

import org.springframework.util.StringUtils;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;

import cz.jirutka.rsql.parser.RSQLParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes" })
public class RSQLQueryDslSupport extends RSQLCommonSupport {

	public RSQLQueryDslSupport() {
		super();
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
			prop.load(getClass().getResourceAsStream("/META-INF/maven/io.github.perplexhub/rsql-jpa-querydsl/pom.properties"));
			String version = prop.getProperty("version");
			return StringUtils.hasText(version) ? "[" + version + "] " : "";
		} catch (Exception e) {
			return "";
		}
	}

}
