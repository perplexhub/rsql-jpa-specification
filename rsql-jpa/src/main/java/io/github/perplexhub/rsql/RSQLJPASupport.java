package io.github.perplexhub.rsql;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "serial" })
public class RSQLJPASupport extends RSQLCommonSupport {

	public RSQLJPASupport() {
		super();
	}

	public RSQLJPASupport(Map<String, EntityManager> entityManagerMap) {
		super(entityManagerMap);
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
					return rsql.accept(new RSQLJPAPredicateConverter(cb, propertyPathMapper), root);
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
					return rsql.accept(new RSQLJPAPredicateConverter(cb, null), root);
				} else
					return null;
			}
		};
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

	protected String getVersion() {
		try {
			Properties prop = new Properties();
			prop.load(getClass().getResourceAsStream("/META-INF/maven/io.github.perplexhub/rsql-jpa/pom.properties"));
			String version = prop.getProperty("version");
			return StringUtils.hasText(version) ? "[" + version + "] " : "";
		} catch (Exception e) {
			return "";
		}
	}

}
