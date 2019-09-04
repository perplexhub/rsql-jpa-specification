package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.*;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RSQLQueryDslPredicateConverter implements RSQLVisitor<BooleanExpression, Path> {

	private static final Map<Class, Class> primitiveToWrapper;

	static {
		Map<Class, Class> map = new HashMap<>();
		map.put(boolean.class, Boolean.class);
		map.put(byte.class, Byte.class);
		map.put(char.class, Character.class);
		map.put(double.class, Double.class);
		map.put(float.class, Float.class);
		map.put(int.class, Integer.class);
		map.put(long.class, Long.class);
		map.put(short.class, Short.class);
		map.put(void.class, Void.class);
		primitiveToWrapper = Collections.unmodifiableMap(map);
	}

	private final Map<Class, Function<String, Object>> valueParserMap;
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
		ManagedType<?> classMetadata = RSQLSupport.getManagedType(entityClass.getType());
		Attribute<?, ?> attribute = null;
		String mappedPropertyPath = "";

		for (String property : propertyPath.split("\\.")) {
			String mappedProperty = RSQLSupport.mapProperty(property, entityClass.getType());
			if (!mappedProperty.equals(property)) {
				RSQLQueryDslContext holder = findPropertyPath(mappedProperty, entityClass);
				attribute = holder.getAttribute();
				mappedPropertyPath += (mappedPropertyPath.length() > 0 ? "." : "") + holder.getPropertyPath();
			} else {
				mappedPropertyPath += (mappedPropertyPath.length() > 0 ? "." : "") + mappedProperty;
				if (!RSQLSupport.hasPropertyName(mappedProperty, classMetadata)) {
					throw new IllegalArgumentException("Unknown property: " + mappedProperty + " from entity " + classMetadata.getJavaType().getName());
				}

				if (RSQLSupport.isAssociationType(mappedProperty, classMetadata)) {
					Class<?> associationType = RSQLSupport.findPropertyType(mappedProperty, classMetadata);
					String previousClass = classMetadata.getJavaType().getName();
					classMetadata = RSQLSupport.getManagedType(associationType);
					log.debug("Create a join between [{}] and [{}].", previousClass, classMetadata.getJavaType().getName());
				} else {
					log.debug("Create property path for type [{}] property [{}].", classMetadata.getJavaType().getName(), mappedProperty);
					if (RSQLSupport.isEmbeddedType(mappedProperty, classMetadata)) {
						Class<?> embeddedType = RSQLSupport.findPropertyType(mappedProperty, classMetadata);
						classMetadata = RSQLSupport.getManagedType(embeddedType);
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
						return Expressions.stringPath(entityClass, property).containsIgnoreCase(argument.toString().replace("*", "").replace("^", ""));
					} else if (argument.toString().contains("*")) {
						return Expressions.stringPath(entityClass, property).contains(argument.toString().replace("*", ""));
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
						return Expressions.stringPath(entityClass, property).containsIgnoreCase(argument.toString().replace("*", "").replace("^", "")).not();
					} else if (argument.toString().contains("*")) {
						return Expressions.stringPath(entityClass, property).contains(argument.toString().replace("*", "")).not();
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

	Object castDynamicClass(Class dynamicClass, String value) {
		log.debug("castDynamicClass(dynamicClass:{},value:{})", dynamicClass, value);

		Object object = null;
		try {
			if (valueParserMap.containsKey(dynamicClass)) {
				object = valueParserMap.get(dynamicClass).apply(value);
			} else if (dynamicClass.equals(UUID.class)) {
				object = UUID.fromString(value);
			} else if (dynamicClass.equals(Date.class) || dynamicClass.equals(java.sql.Date.class)) {
				object = java.sql.Date.valueOf(LocalDate.parse(value));
			} else if (dynamicClass.equals(LocalDate.class)) {
				object = LocalDate.parse(value);
			} else if (dynamicClass.equals(LocalDateTime.class)) {
				object = LocalDateTime.parse(value);
			} else if (dynamicClass.equals(OffsetDateTime.class)) {
				object = OffsetDateTime.parse(value);
			} else if (dynamicClass.equals(ZonedDateTime.class)) {
				object = ZonedDateTime.parse(value);
			} else if (dynamicClass.equals(Character.class)) {
				object = (!StringUtils.isEmpty(value) ? value.charAt(0) : null);
			} else if (dynamicClass.equals(boolean.class) || dynamicClass.equals(Boolean.class)) {
				object = Boolean.valueOf(value);
			} else if (dynamicClass.isEnum()) {
				object = Enum.valueOf(dynamicClass, value);
			} else {
				Constructor<?> cons = (Constructor<?>) dynamicClass.getConstructor(new Class<?>[] { String.class });
				object = cons.newInstance(new Object[] { value });
			}

			return object;
		} catch (DateTimeParseException | IllegalArgumentException e) {
			log.debug("Parsing [{}] with [{}] causing [{}], skip", value, dynamicClass.getName(), e.getMessage());
		} catch (Exception e) {
			log.error("Parsing [{}] with [{}] causing [{}], add your value parser via RSQLSupport.addEntityAttributeParser(Type.class, Type::valueOf)", value, dynamicClass.getName(), e.getMessage(), e);
		}
		return null;
	}

}
