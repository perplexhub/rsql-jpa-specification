package io.github.perplexhub.rsql.jpa;

import java.util.Set;
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
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.tennaito.rsql.builder.BuilderTools;
import com.github.tennaito.rsql.builder.SimpleBuilderTools;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;

@Component
@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
public class RsqlJpaSpecification {
	private static final Logger logger = Logger.getLogger(RsqlJpaSpecification.class.getName());
	private static EntityManager entityManager;

	@Autowired
	public RsqlJpaSpecification(EntityManager entityManager) {
		RsqlJpaSpecification.entityManager = entityManager;
	}

	// clone from com.putracode.utils.JPARsqlConverter
	public static <T> Specification<T> rsql(final String rsqlQuery) {
		return new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				if (StringUtils.hasText(rsqlQuery)) {
					Node rsql = new RSQLParser().parse(rsqlQuery);
					return rsql.accept(new RsqlJpaConverter(cb), root);
				} else
					return null;
			}
		};
	}

	public static EntityManager getEntityManager() {
		return entityManager;
	}

	public static <T> RsqlJpaHolder<?, ?> findPropertyPath(String propertyPath, Path startRoot) {
		return findPropertyPath(propertyPath, startRoot, new SimpleBuilderTools());
	}

	// clone from com.github.tennaito.rsql.jpa.PredicateBuilder.findPropertyPath(String, Path, EntityManager, BuilderTools)
	public static <T> RsqlJpaHolder<?, ?> findPropertyPath(String propertyPath, Path startRoot, BuilderTools misc) {
		String[] graph = propertyPath.split("\\.");

		Metamodel metaModel = entityManager.getMetamodel();
		ManagedType<?> classMetadata = metaModel.managedType(startRoot.getJavaType());

		Path<?> root = startRoot;
		Attribute<?, ?> attribute = null;

		for (String property : graph) {
			String mappedProperty = misc.getPropertiesMapper().translate(property, classMetadata.getJavaType());
			if (!mappedProperty.equals(property)) {
				RsqlJpaHolder _holder = findPropertyPath(mappedProperty, root, misc);
				root = _holder.path;
				attribute = _holder.attribute;
			} else {
				if (!hasPropertyName(mappedProperty, classMetadata)) {
					throw new IllegalArgumentException("Unknown property: " + mappedProperty + " from entity " + classMetadata.getJavaType().getName());
				}

				if (isAssociationType(mappedProperty, classMetadata)) {
					Class<?> associationType = findPropertyType(mappedProperty, classMetadata);
					String previousClass = classMetadata.getJavaType().getName();
					classMetadata = metaModel.managedType(associationType);
					logger.log(Level.INFO, "Create a join between {0} and {1}.", new Object[]{previousClass, classMetadata.getJavaType().getName()});

					if (root instanceof Join) {
						root = root.get(mappedProperty);
					} else {
						root = ((From) root).join(mappedProperty);
					}
				} else {
					logger.log(Level.INFO, "Create property path for type {0} property {1}.", new Object[]{classMetadata.getJavaType().getName(), mappedProperty});
					root = root.get(mappedProperty);

					if (isEmbeddedType(mappedProperty, classMetadata)) {
						Class<?> embeddedType = findPropertyType(mappedProperty, classMetadata);
						classMetadata = metaModel.managedType(embeddedType);
					}
					attribute = classMetadata.getAttribute(property);
				}
			}
		}
		RsqlJpaHolder holder = new RsqlJpaHolder<>();
		holder.path = root;
		holder.attribute = attribute;
		return holder;
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
