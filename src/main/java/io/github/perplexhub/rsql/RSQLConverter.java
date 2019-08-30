package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.*;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.StringUtils;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.extern.slf4j.Slf4j;

// clone from com.putracode.utils.JPARsqlConverter
@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RSQLConverter implements RSQLVisitor<Predicate, Root> {

	private static final Map<Class, Class> primitiveToWrapper = new HashMap<>();

	static {
		primitiveToWrapper.put(boolean.class, Boolean.class);
		primitiveToWrapper.put(byte.class, Byte.class);
		primitiveToWrapper.put(char.class, Character.class);
		primitiveToWrapper.put(double.class, Double.class);
		primitiveToWrapper.put(float.class, Float.class);
		primitiveToWrapper.put(int.class, Integer.class);
		primitiveToWrapper.put(long.class, Long.class);
		primitiveToWrapper.put(short.class, Short.class);
		primitiveToWrapper.put(void.class, Void.class);
	}

	private final CriteriaBuilder builder;
	private final Map<Class, Function<String, Object>> valueParserMap;
	private final ConversionService conversionService = new DefaultConversionService();

	public RSQLConverter(CriteriaBuilder builder, Map<Class, Function<String, Object>> valueParserMap) {
		this.builder = builder;
		this.valueParserMap = valueParserMap;
	}

	public Predicate visit(AndNode node, Root root) {
		return builder.and(processNodes(node.getChildren(), root));
	}

	public Predicate visit(OrNode node, Root root) {
		return builder.or(processNodes(node.getChildren(), root));
	}

	public Predicate visit(ComparisonNode node, Root root) {
		ComparisonOperator op = node.getOperator();
		RSQLContext holder = RSQLSupport.findPropertyPath(node.getSelector(), root);
		Path attrPath = holder.getPath();
		Attribute attribute = holder.getAttribute();
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
				return attrPath.in(listObject);
			} else {
				return builder.not(attrPath.in(listObject));
			}
		} else {
			Object argument = castDynamicClass(type, node.getArguments().get(0));
			if (op.equals(IS_NULL)) {
				return builder.isNull(attrPath);
			}
			if (op.equals(NOT_NULL)) {
				return builder.isNotNull(attrPath);
			}
			if (op.equals(IN)) {
				return builder.equal(attrPath, argument);
			}
			if (op.equals(NOT_IN)) {
				return builder.notEqual(attrPath, argument);
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

	Predicate[] processNodes(List<Node> nodes, Root root) {
		Predicate[] predicates = new Predicate[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			predicates[i] = nodes.get(i).accept(this, root);
		}
		return predicates;
	}

	Object castDynamicClass(Class dynamicClass, String value) {
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
