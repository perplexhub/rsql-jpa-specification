package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RSQLQueryDslPredicateConverter extends RSQLVisitorBase<BooleanExpression, Path> {

	private final ConversionService conversionService = new DefaultConversionService();

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

	RSQLQueryDslContext findPropertyPath(String propertyPath, Path entityClass) {
		ManagedType<?> classMetadata = getManagedType(entityClass.getType());
		Attribute<?, ?> attribute = null;
		String mappedPropertyPath = "";

		for (String property : propertyPath.split("\\.")) {
			String mappedProperty = mapProperty(property, entityClass.getType());
			if (!mappedProperty.equals(property)) {
				RSQLQueryDslContext holder = findPropertyPath(mappedProperty, entityClass);
				attribute = holder.getAttribute();
				mappedPropertyPath += (mappedPropertyPath.length() > 0 ? "." : "") + holder.getPropertyPath();
			} else {
				mappedPropertyPath += (mappedPropertyPath.length() > 0 ? "." : "") + mappedProperty;
				if (!hasPropertyName(mappedProperty, classMetadata)) {
					throw new IllegalArgumentException("Unknown property: " + mappedProperty + " from entity " + classMetadata.getJavaType().getName());
				}

				if (isAssociationType(mappedProperty, classMetadata)) {
					Class<?> associationType = findPropertyType(mappedProperty, classMetadata);
					String previousClass = classMetadata.getJavaType().getName();
					classMetadata = getManagedType(associationType);
					log.debug("Create a join between [{}] and [{}].", previousClass, classMetadata.getJavaType().getName());
				} else {
					log.debug("Create property path for type [{}] property [{}].", classMetadata.getJavaType().getName(), mappedProperty);
					if (isEmbeddedType(mappedProperty, classMetadata)) {
						Class<?> embeddedType = findPropertyType(mappedProperty, classMetadata);
						classMetadata = getManagedType(embeddedType);
					}
					attribute = classMetadata.getAttribute(property);
				}
			}
		}
		return RSQLQueryDslContext.of(mappedPropertyPath, attribute);
	}

	@Override
	public BooleanExpression visit(ComparisonNode node, Path entityClass) {
		log.debug("visit(node:{},param:{})", node, entityClass);

		ComparisonOperator op = node.getOperator();
		RSQLQueryDslContext holder = findPropertyPath(node.getSelector(), entityClass);
		Attribute attribute = holder.getAttribute();
		String property = holder.getPropertyPath();
		Class type = attribute.getJavaType();
		if (type.isPrimitive()) {
			type = primitiveToWrapper.get(type);
		}
		if (node.getArguments().size() > 1) {
			List<Object> listObject = new ArrayList<>();
			for (String argument : node.getArguments()) {
				listObject.add(castDynamicClass(type, argument));
			}
			if (op.equals(IN)) {
				return Expressions.path(type, entityClass, property).in(listObject);
			} else {
				return Expressions.path(type, entityClass, property).notIn(listObject);
			}
		} else {
			Object argument = castDynamicClass(type, node.getArguments().get(0));
			if (op.equals(IS_NULL)) {
				return Expressions.path(type, entityClass, property).isNull();
			}
			if (op.equals(NOT_NULL)) {
				return Expressions.path(type, entityClass, property).isNotNull();
			}
			if (op.equals(IN)) {
				return Expressions.path(type, entityClass, property).in(argument);
			}
			if (op.equals(NOT_IN)) {
				return Expressions.path(type, entityClass, property).notIn(argument);
			}
			if (op.equals(EQUAL)) {
				if (type.equals(String.class)) {
					if (argument.toString().contains("*") && argument.toString().contains("^")) {
						return Expressions.stringPath(entityClass, property).likeIgnoreCase(argument.toString().replace("*", "%").replace("^", ""));
					} else if (argument.toString().contains("*")) {
						return Expressions.stringPath(entityClass, property).like(argument.toString().replace("*", "%"));
					} else if (argument.toString().contains("^")) {
						return Expressions.stringPath(entityClass, property).equalsIgnoreCase(argument.toString().replace("^", ""));
					} else {
						return Expressions.stringPath(entityClass, property).eq(argument.toString());
					}
				} else if (argument == null) {
					return Expressions.path(type, entityClass, property).isNull();
				} else {
					return Expressions.path(type, entityClass, property).eq(argument);
				}
			}
			if (op.equals(NOT_EQUAL)) {
				if (type.equals(String.class)) {
					if (argument.toString().contains("*") && argument.toString().contains("^")) {
						return Expressions.stringPath(entityClass, property).likeIgnoreCase(argument.toString().replace("*", "%").replace("^", "")).not();
					} else if (argument.toString().contains("*")) {
						return Expressions.stringPath(entityClass, property).like(argument.toString().replace("*", "%")).not();
					} else if (argument.toString().contains("^")) {
						return Expressions.stringPath(entityClass, property).equalsIgnoreCase(argument.toString().replace("^", "")).not();
					} else {
						return Expressions.stringPath(entityClass, property).eq(argument.toString()).not();
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
			Comparable comparable = (Comparable) conversionService.convert(argument, type);

			if (op.equals(GREATER_THAN)) {
				return Expressions.comparableEntityPath(type, entityClass, property).gt(comparable);
			}
			if (op.equals(GREATER_THAN_OR_EQUAL)) {
				return Expressions.comparableEntityPath(type, entityClass, property).goe(comparable);
			}
			if (op.equals(LESS_THAN)) {
				return Expressions.comparableEntityPath(type, entityClass, property).lt(comparable);
			}
			if (op.equals(LESS_THAN_OR_EQUAL)) {
				return Expressions.comparableEntityPath(type, entityClass, property).loe(comparable);
			}
		}
		log.error("Unknown operator: {}", op);
		throw new IllegalArgumentException("Unknown operator: " + op);
	}

}
