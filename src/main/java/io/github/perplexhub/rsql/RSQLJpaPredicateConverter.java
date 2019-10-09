package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RSQLJpaPredicateConverter extends RSQLVisitorBase<Predicate, Root> {

	private final CriteriaBuilder builder;
	private final ConversionService conversionService = new DefaultConversionService();
	private final Map<String, Path> cachedJoins = new HashMap<>();

	<T> RSQLJpaContext findPropertyPath(String propertyPath, Path startRoot) {
		ManagedType<?> classMetadata = getManagedType(startRoot.getJavaType());
		Path<?> root = startRoot;
		Attribute<?, ?> attribute = null;

		String[] properties = propertyPath.split("\\.");

		for (String property : properties) {
			String mappedProperty = mapProperty(property, classMetadata.getJavaType());
			if (!mappedProperty.equals(property)) {
				RSQLJpaContext context = findPropertyPath(mappedProperty, root);
				root = context.getPath();
				attribute = context.getAttribute();
			} else {
				if (!hasPropertyName(mappedProperty, classMetadata)) {
					throw new IllegalArgumentException("Unknown property: " + mappedProperty + " from entity " + classMetadata.getJavaType().getName());
				}

				if (isAssociationType(mappedProperty, classMetadata)) {
					Class<?> associationType = findPropertyType(mappedProperty, classMetadata);
					String previousClass = classMetadata.getJavaType().getName();
					classMetadata = getManagedType(associationType);
					log.debug("Create a join between [{}] and [{}].", previousClass, classMetadata.getJavaType().getName());

					if (root instanceof Join) {
						root = root.get(mappedProperty);
					} else {
						String keyJoin = startRoot.getJavaType().getSimpleName().concat(".").concat(mappedProperty);
						if (cachedJoins.containsKey(keyJoin)) {
							root = cachedJoins.get(keyJoin);
						} else {
							root = ((From) root).join(mappedProperty);
							cachedJoins.put(keyJoin, root);
						}
					}
				} else {
					log.debug("Create property path for type [{}] property [{}].", classMetadata.getJavaType().getName(), mappedProperty);
					root = root.get(mappedProperty);

					if (isEmbeddedType(mappedProperty, classMetadata)) {
						Class<?> embeddedType = findPropertyType(mappedProperty, classMetadata);
						classMetadata = getManagedType(embeddedType);
					} else {
						attribute = classMetadata.getAttribute(property);
					}
				}
			}
		}
		return RSQLJpaContext.of(root, attribute);
	}

	@Override
	public Predicate visit(ComparisonNode node, Root root) {
		log.debug("visit(node:{},root:{})", node, root);

		ComparisonOperator op = node.getOperator();
		RSQLJpaContext holder = findPropertyPath(node.getSelector(), root);
		Path attrPath = holder.getPath();
		Attribute attribute = holder.getAttribute();
		Class type = attribute.getJavaType();
		if (type.isPrimitive()) {
			type = primitiveToWrapper.get(type);
		} else if (RSQLSupport.getValueTypeMap().containsKey(type)) {
			type = RSQLSupport.getValueTypeMap().get(type); // if you want to treat Enum as String and apply like search, etc
		}

		if (node.getArguments().size() > 1) {
			List<Object> listObject = new ArrayList<>();
			for (String argument : node.getArguments()) {
				listObject.add(castDynamicClass(type, argument));
			}
			if (op.equals(IN)) {
				return attrPath.in(listObject);
			}
			if (op.equals(NOT_IN)) {
				return attrPath.in(listObject).not();
			}
			if (op.equals(BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
				return builder.between(attrPath, (Comparable) listObject.get(0), (Comparable) listObject.get(1));
			}
			if (op.equals(NOT_BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
				return builder.between(attrPath, (Comparable) listObject.get(0), (Comparable) listObject.get(1)).not();
			}
		} else {
			if (op.equals(IS_NULL)) {
				return builder.isNull(attrPath);
			}
			if (op.equals(NOT_NULL)) {
				return builder.isNotNull(attrPath);
			}
			Object argument = castDynamicClass(type, node.getArguments().get(0));
			if (op.equals(IN)) {
				return builder.equal(attrPath, argument);
			}
			if (op.equals(NOT_IN)) {
				return builder.notEqual(attrPath, argument);
			}
			if (op.equals(LIKE)) {
				return builder.like(attrPath, "%" + argument.toString() + "%");
			}
			if (op.equals(NOT_LIKE)) {
				return builder.like(attrPath, "%" + argument.toString() + "%").not();
			}
			if (op.equals(IGNORE_CASE)) {
				return builder.equal(builder.upper(attrPath), argument.toString().toUpperCase());
			}
			if (op.equals(IGNORE_CASE_LIKE)) {
				return builder.like(builder.upper(attrPath), "%" + argument.toString().toUpperCase() + "%");
			}
			if (op.equals(IGNORE_CASE_NOT_LIKE)) {
				return builder.like(builder.upper(attrPath), "%" + argument.toString().toUpperCase() + "%").not();
			}
			if (op.equals(EQUAL)) {
				if (type.equals(String.class)) {
					if (argument.toString().contains("*") && argument.toString().contains("^")) {
						return builder.like(builder.upper(attrPath), argument.toString().replace("*", "%").replace("^", "").toUpperCase());
					} else if (argument.toString().contains("*")) {
						return builder.like(attrPath, argument.toString().replace('*', '%'));
					} else if (argument.toString().contains("^")) {
						return builder.equal(builder.upper(attrPath), argument.toString().replace("^", "").toUpperCase());
					} else {
						return builder.equal(attrPath, argument);
					}
				} else if (argument == null) {
					return builder.isNull(attrPath);
				} else {
					return builder.equal(attrPath, argument);
				}
			}
			if (op.equals(NOT_EQUAL)) {
				if (type.equals(String.class)) {
					if (argument.toString().contains("*") && argument.toString().contains("^")) {
						return builder.notLike(builder.upper(attrPath), argument.toString().replace("*", "%").replace("^", "").toUpperCase());
					} else if (argument.toString().contains("*")) {
						return builder.notLike(attrPath, argument.toString().replace('*', '%'));
					} else if (argument.toString().contains("^")) {
						return builder.notEqual(builder.upper(attrPath), argument.toString().replace("^", "").toUpperCase());
					} else {
						return builder.notEqual(attrPath, argument);
					}
				} else if (argument == null) {
					return builder.isNotNull(attrPath);
				} else {
					return builder.notEqual(attrPath, argument);
				}
			}
			if (!Comparable.class.isAssignableFrom(type)) {
				log.error("Operator {} can be used only for Comparables", op);
				throw new IllegalArgumentException(String.format("Operator %s can be used only for Comparables", op));
			}
			Comparable comparable = (Comparable) conversionService.convert(argument, type);

			if (op.equals(GREATER_THAN)) {
				return builder.greaterThan(attrPath, comparable);
			}
			if (op.equals(GREATER_THAN_OR_EQUAL)) {
				return builder.greaterThanOrEqualTo(attrPath, comparable);
			}
			if (op.equals(LESS_THAN)) {
				return builder.lessThan(attrPath, comparable);
			}
			if (op.equals(LESS_THAN_OR_EQUAL)) {
				return builder.lessThanOrEqualTo(attrPath, comparable);
			}
		}
		log.error("Unknown operator: {}", op);
		throw new IllegalArgumentException("Unknown operator: " + op);
	}

	@Override
	public Predicate visit(AndNode node, Root root) {
		log.debug("visit(node:{},root:{})", node, root);

		return node.getChildren().stream().map(n -> n.accept(this, root)).collect(Collectors.reducing(builder::and)).get();
	}

	@Override
	public Predicate visit(OrNode node, Root root) {
		log.debug("visit(node:{},root:{})", node, root);

		return node.getChildren().stream().map(n -> n.accept(this, root)).collect(Collectors.reducing(builder::or)).get();
	}

}
