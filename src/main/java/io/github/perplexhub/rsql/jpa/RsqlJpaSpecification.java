package io.github.perplexhub.rsql.jpa;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;

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

@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
public class RsqlJpaSpecification {
	private static final Logger logger = Logger.getLogger(RsqlJpaSpecification.class.getName());
	private static final Map<Class, ManagedType> managedTypeMap = new ConcurrentHashMap<>();
	private static final Map<Class, Function<String, Object>> valueParserMap = new ConcurrentHashMap<>();
	private static Map<String, EntityManager> entityManagerMap = Collections.emptyMap();
	private static Map<Class<?>, Map<String, String>> propertyRemapping = new ConcurrentHashMap<>();

	public RsqlJpaSpecification(Map<String, EntityManager> entityManagerMap) {
		RsqlJpaSpecification.entityManagerMap = entityManagerMap;
	}

	public void addEntityAttributeParser(Class valueClass, Function<String, Object> function) {
		if (valueClass != null && function != null) {
			RsqlJpaSpecification.valueParserMap.put(valueClass, function);
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
		return jpaSpecificationExecutor.findOne(rsql(rsqlQuery));
	}

	/**
	 * Returns all entities matching the given {@link Specification}.
	 *
	 * @param jpaSpecificationExecutor JPA repository
	 * @param rsqlQuery can be {@literal null}.
	 * @return never {@literal null}.
	 */
	public static List<?> findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
		return jpaSpecificationExecutor.findAll(rsql(rsqlQuery));
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
		return jpaSpecificationExecutor.findAll(rsql(rsqlQuery), pageable);
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
		return jpaSpecificationExecutor.findAll(rsql(rsqlQuery), sort);
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
				? jpaSpecificationExecutor.findAll(rsql(rsqlQuery), Sort.by(Direction.ASC, StringUtils.commaDelimitedListToStringArray(sort)))
				: jpaSpecificationExecutor.findAll(rsql(rsqlQuery));
	}

	/**
	 * Returns the number of instances that the given {@link Specification} will return.
	 *
	 * @param jpaSpecificationExecutor JPA repository
	 * @param rsqlQuery the {@link Specification} to count instances for. Can be {@literal null}.
	 * @return the number of instances.
	 */
	public static long count(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
		return jpaSpecificationExecutor.count(rsql(rsqlQuery));
	}

	// clone from com.putracode.utils.JPARsqlConverter
	public static <T> Specification<T> rsql(final String rsqlQuery) {
		return new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				if (StringUtils.hasText(rsqlQuery)) {
					Node rsql = new RSQLParser().parse(rsqlQuery);
					return rsql.accept(new RsqlJpaConverter(cb, valueParserMap), root);
				} else
					return null;
			}
		};
	}

	public static <T> RsqlJpaHolder<?, ?> findPropertyPath(String propertyPath, Path startRoot) {
		String[] graph = propertyPath.split("\\.");

		ManagedType<?> classMetadata = getManagedType(startRoot.getJavaType());

		Path<?> root = startRoot;
		Attribute<?, ?> attribute = null;

		for (String property : graph) {
			String mappedProperty = mapProperty(property, classMetadata.getJavaType());
			if (!mappedProperty.equals(property)) {
				RsqlJpaHolder _holder = findPropertyPath(mappedProperty, root);
				root = _holder.getPath();
				attribute = _holder.getAttribute();
			} else {
				if (!hasPropertyName(mappedProperty, classMetadata)) {
					throw new IllegalArgumentException("Unknown property: " + mappedProperty + " from entity " + classMetadata.getJavaType().getName());
				}

				if (isAssociationType(mappedProperty, classMetadata)) {
					Class<?> associationType = findPropertyType(mappedProperty, classMetadata);
					String previousClass = classMetadata.getJavaType().getName();
					classMetadata = getManagedType(associationType);
					logger.log(Level.FINE, "Create a join between {0} and {1}.", new Object[] { previousClass, classMetadata.getJavaType().getName() });

					if (root instanceof Join) {
						root = root.get(mappedProperty);
					} else {
						root = ((From) root).join(mappedProperty);
					}
				} else {
					logger.log(Level.FINE, "Create property path for type {0} property {1}.", new Object[] { classMetadata.getJavaType().getName(), mappedProperty });
					root = root.get(mappedProperty);

					if (isEmbeddedType(mappedProperty, classMetadata)) {
						Class<?> embeddedType = findPropertyType(mappedProperty, classMetadata);
						classMetadata = getManagedType(embeddedType);
					}
					attribute = classMetadata.getAttribute(property);
				}
			}
		}
		RsqlJpaHolder holder = new RsqlJpaHolder<>();
		holder.setPath(root);
		holder.setAttribute(attribute);
		return holder;
	}

	private static String mapProperty(String selector, Class<?> entityClass) {
		if (!propertyRemapping.isEmpty()) {
			Map<String, String> map = propertyRemapping.get(entityClass);
			String property = (map != null) ? map.get(selector) : null;

			if (property != null) {
				logger.log(Level.INFO, "Found mapping {0} -> {1}", new Object[] { selector, property });
				return property;
			}
		}
		return selector;
	}

	public static void addMapping(Class<?> entityClass, Map<String, String> mapping) {
		propertyRemapping.put(entityClass, mapping);
	}

	public static void addMapping(Class<?> entityClass, String selector, String property) {
		propertyRemapping.computeIfAbsent(entityClass, entityClazz -> new ConcurrentHashMap<>()).put(selector, property);
	}

	private static <T> ManagedType<T> getManagedType(Class<T> cls) {
		Exception _e = null;
		if (entityManagerMap.size() > 0) {
			ManagedType<T> managedType = managedTypeMap.get(cls);
			if (managedType != null) {
				logger.log(Level.FINE, "Found managed type [{0}] in cache", new Object[] { cls });
				return managedType;
			}
			for (Entry<String, EntityManager> entityManagerEntry : entityManagerMap.entrySet()) {
				try {
					managedType = entityManagerEntry.getValue().getMetamodel().managedType(cls);
					managedTypeMap.put(cls, managedType);
					logger.log(Level.INFO, "Found managed type [{0}] in EntityManager [{1}]", new Object[] { cls, entityManagerEntry.getKey() });
					return managedType;
				} catch (Exception e) {
					if (e != null) {
						_e = e;
					}
					logger.log(Level.FINE, "[{0}] not found in EntityManager [{1}] due to [{2}]", new Object[] { cls, entityManagerEntry.getKey(), e == null ? "-" : e.getMessage() });
				}
			}
		}
		logger.log(Level.SEVERE, "[{0}] not found in EntityManager{}: [{1}]", new Object[] { cls, entityManagerMap.size() > 1 ? "s" : "", StringUtils.collectionToCommaDelimitedString(entityManagerMap.keySet()) });
		throw _e != null ? new RuntimeException(_e) : new IllegalStateException("No entity manager bean found in application context");
	}

	private static <T> Class<?> findPropertyType(String property, ManagedType<T> classMetadata) {
		Class<?> propertyType = null;
		if (classMetadata.getAttribute(property).isCollection()) {
			propertyType = ((PluralAttribute) classMetadata.getAttribute(property)).getBindableJavaType();
		} else {
			propertyType = classMetadata.getAttribute(property).getJavaType();
		}
		return propertyType;
	}

	private static <T> boolean hasPropertyName(String property, ManagedType<T> classMetadata) {
		Set<Attribute<? super T, ?>> names = classMetadata.getAttributes();
		for (Attribute<? super T, ?> name : names) {
			if (name.getName().equals(property))
				return true;
		}
		return false;
	}

	private static <T> boolean isEmbeddedType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).getPersistentAttributeType() == PersistentAttributeType.EMBEDDED;
	}

	private static <T> boolean isAssociationType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).isAssociation();
	}
}
