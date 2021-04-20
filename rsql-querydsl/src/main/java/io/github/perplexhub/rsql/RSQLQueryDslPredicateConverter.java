package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.*;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RSQLQueryDslPredicateConverter extends RSQLVisitorBase<BooleanExpression, Path> {

	private final @Getter Map<String, String> propertyPathMapper;

	public RSQLQueryDslPredicateConverter(Map<String, String> propertyPathMapper) {
		super();
		this.propertyPathMapper = propertyPathMapper != null ? propertyPathMapper : Collections.emptyMap();
	}

	@SneakyThrows
	RSQLQueryDslContext findPropertyPath(String propertyPath, Path entityClass) {
		Path path = entityClass;
		ManagedType<?> classMetadata = getManagedType(path.getType());
		Attribute<?, ?> attribute = null;
		String mappedPropertyPath = "";

		String[] properties = propertyPath.split("\\.");

		for (String property : properties) {
			String mappedProperty = mapProperty(property, path.getType());
			if (!mappedProperty.equals(property)) {
				RSQLQueryDslContext holder = findPropertyPath(mappedProperty, path);
				attribute = holder.getAttribute();
				mappedPropertyPath += (mappedPropertyPath.length() > 0 ? "." : "") + holder.getPropertyPath();
			} else {
				if (!hasPropertyName(mappedProperty, classMetadata)) {
					throw new IllegalArgumentException("Unknown property: " + mappedProperty + " from entity " + classMetadata.getJavaType().getName());
				}

				if (isAssociationType(mappedProperty, classMetadata)) {
					Class<?> associationType = findPropertyType(mappedProperty, classMetadata);
					String previousClass = classMetadata.getJavaType().getName();
					classMetadata = getManagedType(associationType);
					log.debug("Create a join between [{}] and [{}].", previousClass, classMetadata.getJavaType().getName());
					path = (Path) path.getClass().getDeclaredField(mappedProperty).get(path);
					if (path instanceof CollectionPathBase) {
						path = (Path) ((CollectionPathBase) path).any();
					}
					mappedPropertyPath = "";
				} else if (isElementCollectionType(mappedProperty, classMetadata)) {
					String previousClass = classMetadata.getJavaType().getName();
					attribute = classMetadata.getAttribute(property);
					classMetadata = getManagedElementCollectionType(mappedProperty, classMetadata);

					if (previousClass.equals(classMetadata.getJavaType().getName())) {
						path = (Path) path.getClass().getDeclaredField(mappedProperty).get(path);
						if (path instanceof CollectionPathBase) {
							path = (Path) ((CollectionPathBase) path).any();
						}
					} else {
						log.debug("Create an element collection join between [{}] and [{}].", previousClass, classMetadata.getJavaType().getName());
						path = (Path) path.getClass().getDeclaredField(mappedProperty).get(path);
						if (path instanceof CollectionPathBase) {
							path = (Path) ((CollectionPathBase) path).any();
						}
						mappedPropertyPath = "";
					}
				} else {
					log.debug("Create property path for type [{}] property [{}].", classMetadata.getJavaType().getName(), mappedProperty);
					if (isEmbeddedType(mappedProperty, classMetadata)) {
						Class<?> embeddedType = findPropertyType(mappedProperty, classMetadata);
						classMetadata = getManagedType(embeddedType);
					} else {
						attribute = classMetadata.getAttribute(property);
					}
					mappedPropertyPath += (mappedPropertyPath.length() > 0 ? "." : "") + mappedProperty;
				}
			}
		}

		if (attribute != null) {
			accessControl(path.getType(), attribute.getName());
		}

		return RSQLQueryDslContext.of(mappedPropertyPath, attribute, path);
	}

	@Override
	@SneakyThrows
	public BooleanExpression visit(ComparisonNode node, Path path) {
		log.debug("visit(node:{},path:{})", node, path);

		ComparisonOperator op = node.getOperator();
		RSQLQueryDslContext holder = findPropertyPath(mapPropertyPath(node.getSelector()), path);
		Attribute attribute = holder.getAttribute();
		String property = holder.getPropertyPath();
		Path entityClass = holder.getEntityClass();
		Class type = attribute.getJavaType();
		if (attribute.getPersistentAttributeType() == PersistentAttributeType.ELEMENT_COLLECTION) {
			type = getElementCollectionGenericType(type, attribute);
		}
		if (type.isPrimitive()) {
			type = primitiveToWrapper.get(type);
		} else if (RSQLQueryDslSupport.getValueTypeMap().containsKey(type)) {
			type = RSQLQueryDslSupport.getValueTypeMap().get(type); // if you want to treat Enum as String and apply like search, etc
		}
		if (node.getArguments().size() > 1) {
			List<Object> listObject = new ArrayList<>();
			for (String argument : node.getArguments()) {
				listObject.add(convert(argument, type));
			}
			if (op.equals(IN)) {
				return Expressions.path(type, entityClass, property).in(listObject);
			}
			if (op.equals(NOT_IN)) {
				return Expressions.path(type, entityClass, property).notIn(listObject);
			}
			if (op.equals(BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
				ComparableEntityPath comparableEntityPath = getComparableEntityPath(type, entityClass, property);
				return comparableEntityPath.between((Comparable) listObject.get(0), (Comparable) listObject.get(1));
			}
			if (op.equals(NOT_BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
				ComparableEntityPath comparableEntityPath = getComparableEntityPath(type, entityClass, property);
				return comparableEntityPath.notBetween((Comparable) listObject.get(0), (Comparable) listObject.get(1));
			}
		} else {
			if (op.equals(IS_NULL)) {
				return Expressions.path(type, entityClass, property).isNull();
			}
			if (op.equals(NOT_NULL)) {
				return Expressions.path(type, entityClass, property).isNotNull();
			}
			Object argument = convert(node.getArguments().get(0), type);
			if (op.equals(IN)) {
				return Expressions.path(type, entityClass, property).in(argument);
			}
			if (op.equals(NOT_IN)) {
				return Expressions.path(type, entityClass, property).notIn(argument);
			}
			if (op.equals(LIKE)) {
				StringExpression stringExpression = getStringExpression(entityClass, property, isEnumPath(entityClass, property));
				return stringExpression.like("%" + argument.toString() + "%");
			}
			if (op.equals(NOT_LIKE)) {
				StringExpression stringExpression = getStringExpression(entityClass, property, isEnumPath(entityClass, property));
				return stringExpression.like("%" + argument.toString() + "%").not();
			}
			if (op.equals(IGNORE_CASE)) {
				StringExpression stringExpression = getStringExpression(entityClass, property, isEnumPath(entityClass, property));
				return stringExpression.equalsIgnoreCase(argument.toString());
			}
			if (op.equals(IGNORE_CASE_LIKE)) {
				StringExpression stringExpression = getStringExpression(entityClass, property, isEnumPath(entityClass, property));
				return stringExpression.likeIgnoreCase("%" + argument.toString() + "%");
			}
			if (op.equals(IGNORE_CASE_NOT_LIKE)) {
				StringExpression stringExpression = getStringExpression(entityClass, property, isEnumPath(entityClass, property));
				return stringExpression.likeIgnoreCase("%" + argument.toString() + "%").not();
			}
			if (op.equals(EQUAL)) {
				if (type.equals(String.class)) {
					StringExpression stringExpression = getStringExpression(entityClass, property, isEnumPath(entityClass, property));

					if (argument.toString().contains("*") && argument.toString().contains("^")) {
						return stringExpression.likeIgnoreCase(argument.toString().replace("*", "%").replace("^", ""));
					} else if (argument.toString().contains("*")) {
						return stringExpression.like(argument.toString().replace("*", "%"));
					} else if (argument.toString().contains("^")) {
						return stringExpression.equalsIgnoreCase(argument.toString().replace("^", ""));
					} else {
						return stringExpression.eq(argument.toString());
					}
				} else if (argument == null) {
					return Expressions.path(type, entityClass, property).isNull();
				} else {
					return Expressions.path(type, entityClass, property).eq(argument);
				}
			}
			if (op.equals(NOT_EQUAL)) {
				if (type.equals(String.class)) {
					StringExpression stringExpression = getStringExpression(entityClass, property, isEnumPath(entityClass, property));

					if (argument.toString().contains("*") && argument.toString().contains("^")) {
						return stringExpression.likeIgnoreCase(argument.toString().replace("*", "%").replace("^", "")).not();
					} else if (argument.toString().contains("*")) {
						return stringExpression.like(argument.toString().replace("*", "%")).not();
					} else if (argument.toString().contains("^")) {
						return stringExpression.equalsIgnoreCase(argument.toString().replace("^", "")).not();
					} else {
						return stringExpression.eq(argument.toString()).not();
					}
				} else if (argument == null) {
					return Expressions.path(type, entityClass, property).isNotNull();
				} else {
					return Expressions.path(type, entityClass, property).eq(argument).not();
				}
			}
			if (!Comparable.class.isAssignableFrom(type)) {
				log.error("Operator {} can be used only for Comparables", op);
				throw new IllegalArgumentException(String.format("Operator %s can be used only for Comparables", op));
			}
			Comparable comparable = (Comparable) argument;
			ComparableEntityPath comparableEntityPath = getComparableEntityPath(type, entityClass, property);

			if (op.equals(GREATER_THAN)) {
				return comparableEntityPath.gt(comparable);
			}
			if (op.equals(GREATER_THAN_OR_EQUAL)) {
				return comparableEntityPath.goe(comparable);
			}
			if (op.equals(LESS_THAN)) {
				return comparableEntityPath.lt(comparable);
			}
			if (op.equals(LESS_THAN_OR_EQUAL)) {
				return comparableEntityPath.loe(comparable);
			}
		}
		log.error("Unknown operator: {}", op);
		throw new IllegalArgumentException("Unknown operator: " + op);
	}

	@Override
	public BooleanExpression visit(AndNode node, Path entityClass) {
		log.debug("visit(node:{},param:{})", node, entityClass);

		return node.getChildren().stream().map(n -> n.accept(this, entityClass)).collect(Collectors.reducing(BooleanExpression::and)).get();
	}

	@Override
	public BooleanExpression visit(OrNode node, Path entityClass) {
		log.debug("visit(node:{},param:{})", node, entityClass);

		return node.getChildren().stream().map(n -> n.accept(this, entityClass)).collect(Collectors.reducing(BooleanExpression::or)).get();
	}

	ComparableEntityPath getComparableEntityPath(Class type, Path entityClass, String property) {
		return Expressions.comparableEntityPath(type, entityClass, property);
	}

	@SneakyThrows
	StringExpression getStringExpression(Path entityClass, String property, boolean isEnumPath) {
		if (entityClass instanceof StringExpression && (property == null || property.isEmpty())) {
			return (StringExpression) entityClass;
		}
		if (isEnumPath) {
			return ((EnumPath) entityClass.getClass().getDeclaredField(property).get(entityClass)).stringValue();
		}
		return Expressions.stringPath(entityClass, property);
	}

	boolean isEnumPath(Path entityClass, String property) {
		try {
			return entityClass.getClass().getDeclaredField(property).get(entityClass) instanceof EnumPath;
		} catch (Exception e) {
			return false;
		}
	}

}
