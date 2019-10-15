package io.github.perplexhub.rsql;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.ManagedType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes", "serial" })
public class RSQLSupport {

	private @Getter static final Map<Class, Function<String, Object>> valueParserMap = new ConcurrentHashMap<>();
	private @Getter static final Map<Class, Class> valueTypeMap = new ConcurrentHashMap<>();
	private @Getter static final Map<Class, ManagedType> managedTypeMap = new ConcurrentHashMap<>();
	private @Getter static final Map<String, EntityManager> entityManagerMap = new ConcurrentHashMap<>();
	private @Getter static final Map<Class<?>, Map<String, String>> propertyRemapping = new ConcurrentHashMap<>();

	public RSQLSupport(Map<String, EntityManager> entityManagerMap) {
		if (entityManagerMap != null) {
			RSQLSupport.entityManagerMap.putAll(entityManagerMap);
			log.info("{} EntityManager bean{} found: {}", entityManagerMap.size(), entityManagerMap.size() > 1 ? "s are" : " is", entityManagerMap);
		} else {
			log.warn("No EntityManager beans are found");
		}
		log.info("RSQLSupport {}is initialized", getVersion());
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
		log.debug("toSpecification({},propertyPathMapper:{})", rsqlQuery, propertyPathMapper);
		return new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				if (StringUtils.hasText(rsqlQuery)) {
					Node rsql = new RSQLParser(RSQLOperators.supportedOperators()).parse(rsqlQuery);
					return rsql.accept(new RSQLJpaPredicateConverter(cb, propertyPathMapper), root);
				} else
					return null;
			}
		};
	}

	public static <T> Specification<T> toSpecification(final String rsqlQuery, final boolean distinct) {
		log.debug("toSpecification({},distinct:{})", rsqlQuery, distinct);
		return new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				query.distinct(distinct);
				if (StringUtils.hasText(rsqlQuery)) {
					Node rsql = new RSQLParser(RSQLOperators.supportedOperators()).parse(rsqlQuery);
					return rsql.accept(new RSQLJpaPredicateConverter(cb, null), root);
				} else
					return null;
			}
		};
	}

	public static com.querydsl.core.types.Predicate toPredicate(final String rsqlQuery, final com.querydsl.core.types.Path qClazz) {
		return toPredicate(rsqlQuery, qClazz, null);
	}

	public static com.querydsl.core.types.Predicate toPredicate(final String rsqlQuery, final com.querydsl.core.types.Path qClazz, final Map<String, String> propertyPathMapper) {
		log.debug("toPredicate({},qClazz:{},propertyPathMapper:{})", rsqlQuery, qClazz);
		if (StringUtils.hasText(rsqlQuery)) {
			return new RSQLParser(RSQLOperators.supportedOperators())
					.parse(rsqlQuery)
					.accept(new RSQLQueryDslPredicateConverter(propertyPathMapper), qClazz);
		} else {
			return null;
		}
	}

	/**
	 * Returns a single entity matching the given {@link Specification} or {@link Optional#empty()} if none found.
	 *
	 * @param jpaSpecificationExecutor JPA repository
	 * @param rsqlQuery can be {@literal null}.
	 * @return never {@literal null}.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one entity found.
	 */
	public static Optional<?> findOne(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
		return jpaSpecificationExecutor.findOne(toSpecification(rsqlQuery));
	}

	/**
	 * Returns all entities matching the given {@link Specification}.
	 *
	 * @param jpaSpecificationExecutor JPA repository
	 * @param rsqlQuery can be {@literal null}.
	 * @return never {@literal null}.
	 */
	public static List<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
		return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery));
	}

	/**
	 * Returns a {@link Page} of entities matching the given {@link Specification}.
	 *
	 * @param jpaSpecificationExecutor JPA repository
	 * @param rsqlQuery can be {@literal null}.
	 * @param pageable must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	public static Page<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, Pageable pageable) {
		return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), pageable);
	}

	/**
	 * Returns all entities matching the given {@link Specification} and {@link Sort}.
	 *
	 * @param jpaSpecificationExecutor JPA repository
	 * @param rsqlQuery can be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	public static List<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, Sort sort) {
		return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), sort);
	}

	/**
	 * Returns all entities matching the given {@link Specification} and {@link Sort}.
	 *
	 * @param jpaSpecificationExecutor JPA repository
	 * @param rsqlQuery can be {@literal null}.
	 * @param sort can be {@literal null}, comma delimited.
	 * @return never {@literal null}.
	 */
	public static List<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, @Nullable String sort) {
		return StringUtils.hasText(sort)
				? jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), Sort.by(Direction.ASC, StringUtils.commaDelimitedListToStringArray(sort)))
				: jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery));
	}

	/**
	 * Returns the number of instances that the given {@link Specification} will return.
	 *
	 * @param jpaSpecificationExecutor JPA repository
	 * @param rsqlQuery the {@link Specification} to count instances for. Can be {@literal null}.
	 * @return the number of instances.
	 */
	public static long count(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
		return jpaSpecificationExecutor.count(toSpecification(rsqlQuery));
	}

	public static void addMapping(Class<?> entityClass, Map<String, String> mapping) {
		log.info("Adding entity class mapping for {}", entityClass);
		propertyRemapping.put(entityClass, mapping);
	}

	public static void addMapping(Class<?> entityClass, String selector, String property) {
		log.info("Adding entity class mapping for {}, selector {} and property {}", entityClass, selector, property);
		propertyRemapping.computeIfAbsent(entityClass, entityClazz -> new ConcurrentHashMap<>()).put(selector, property);
	}

	public static void addEntityAttributeParser(Class valueClass, Function<String, Object> function) {
		log.info("Adding entity attribute parser for {}", valueClass);
		if (valueClass != null && function != null) {
			RSQLSupport.valueParserMap.put(valueClass, function);
		}
	}

	public static void addEntityAttributeTypeMap(Class valueClass, Class mappedClass) {
		log.info("Adding entity attribute type map for {} -> {}", valueClass, mappedClass);
		if (valueClass != null && mappedClass != null) {
			RSQLSupport.valueTypeMap.put(valueClass, mappedClass);
		}
	}

	protected String getVersion() {
		try {
			Properties prop = new Properties();
			prop.load(getClass().getResourceAsStream("/META-INF/maven/io.github.perplexhub/rsql-jpa-specification/pom.properties"));
			String version = prop.getProperty("version");
			return StringUtils.hasText(version) ? "[" + version + "] " : "";
		} catch (Exception e) {
			return "";
		}
	}

}
