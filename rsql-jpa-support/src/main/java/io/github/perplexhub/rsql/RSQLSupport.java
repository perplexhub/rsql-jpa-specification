package io.github.perplexhub.rsql;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.dsl.BooleanExpression;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes" })
public class RSQLSupport {

	public RSQLSupport() {
		log.info("RSQLSupport is initialized.");
	}

	public static <T> Specification<T> rsql(final String rsqlQuery) {
		return toSpecification(rsqlQuery);
	}

	public static <T> Specification<T> rsql(final String rsqlQuery, final boolean distinct) {
		return toSpecification(rsqlQuery, distinct);
	}

	public static <T> Specification<T> toSpecification(final String rsqlQuery) {
		return toSpecification(rsqlQuery, null);
	}

	public static <T> Specification<T> toSpecification(final String rsqlQuery, final Map<String, String> propertyPathMapper) {
		return RSQLSpecificationSupport.toSpecification(rsqlQuery, propertyPathMapper);
	}

	public static <T> Specification<T> toSpecification(final String rsqlQuery, final boolean distinct) {
		return RSQLSpecificationSupport.toSpecification(rsqlQuery, distinct);
	}

	public static BooleanExpression toPredicate(final String rsqlQuery, final com.querydsl.core.types.Path qClazz) {
		return toPredicate(rsqlQuery, qClazz, null);
	}

	public static BooleanExpression toPredicate(final String rsqlQuery, final com.querydsl.core.types.Path qClazz, final Map<String, String> propertyPathMapper) {
		return RSQLQueryDslSupport.toPredicate(rsqlQuery, qClazz, propertyPathMapper);
	}

	public static MultiValueMap<String, String> toMultiValueMap(final String rsqlQuery) {
		return RSQLCommonSupport.toMultiValueMap(rsqlQuery);
	}

	public static Map<String, MultiValueMap<String, String>> toComplexMultiValueMap(final String rsqlQuery) {
		return RSQLCommonSupport.toComplexMultiValueMap(rsqlQuery);
	}

	public static Optional<?> findOne(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
		return jpaSpecificationExecutor.findOne(toSpecification(rsqlQuery));
	}

	public static List<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
		return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery));
	}

	public static Page<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, Pageable pageable) {
		return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), pageable);
	}

	public static List<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, Sort sort) {
		return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), sort);
	}

	public static List<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, @Nullable String sort) {
		return StringUtils.hasText(sort)
				? jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), Sort.by(Direction.ASC, StringUtils.commaDelimitedListToStringArray(sort)))
				: jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery));
	}

	public static long count(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
		return jpaSpecificationExecutor.count(toSpecification(rsqlQuery));
	}

	public static void addMapping(Class<?> entityClass, Map<String, String> mapping) {
		RSQLCommonSupport.addMapping(entityClass, mapping);
	}

	public static void addMapping(Class<?> entityClass, String selector, String property) {
		RSQLCommonSupport.addMapping(entityClass, selector, property);
	}

	public static void addEntityAttributeParser(Class valueClass, Function<String, Object> function) {
		RSQLCommonSupport.addEntityAttributeParser(valueClass, function);
	}

	public static void addEntityAttributeTypeMap(Class valueClass, Class mappedClass) {
		RSQLCommonSupport.addEntityAttributeTypeMap(valueClass, mappedClass);
	}

	protected String getVersion() {
		try {
			Properties prop = new Properties();
			prop.load(getClass().getResourceAsStream("/META-INF/maven/io.github.perplexhub/rsql-jpa-support/pom.properties"));
			String version = prop.getProperty("version");
			return StringUtils.hasText(version) ? "[" + version + "] " : "";
		} catch (Exception e) {
			return "";
		}
	}

}
