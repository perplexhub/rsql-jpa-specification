package io.github.perplexhub.rsql.jpa;

import static cz.jirutka.rsql.parser.ast.RSQLOperators.EQUAL;
import static cz.jirutka.rsql.parser.ast.RSQLOperators.GREATER_THAN;
import static cz.jirutka.rsql.parser.ast.RSQLOperators.GREATER_THAN_OR_EQUAL;
import static cz.jirutka.rsql.parser.ast.RSQLOperators.IN;
import static cz.jirutka.rsql.parser.ast.RSQLOperators.LESS_THAN;
import static cz.jirutka.rsql.parser.ast.RSQLOperators.LESS_THAN_OR_EQUAL;
import static cz.jirutka.rsql.parser.ast.RSQLOperators.NOT_EQUAL;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

// clone from com.putracode.utils.JPARsqlConverter
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RsqlJpaConverter implements RSQLVisitor<Predicate, Root> {
	private final CriteriaBuilder builder;
	private final ConversionService conversionService = new DefaultConversionService();

	public RsqlJpaConverter(CriteriaBuilder builder) {
		this.builder = builder;
	}

	public Predicate visit(AndNode node, Root root) {

		return builder.and(processNodes(node.getChildren(), root));
	}

	public Predicate visit(OrNode node, Root root) {

		return builder.or(processNodes(node.getChildren(), root));
	}

	public Predicate visit(ComparisonNode node, Root root) {
		ComparisonOperator op = node.getOperator();
		//Path attrPath = root.get(node.getSelector());
		//Attribute attribute = root.getModel().getAttribute(node.getSelector());
		RsqlJpaHolder holder = RsqlJpaSpecification.findPropertyPath(node.getSelector(), root);
		Path attrPath = holder.path;
		Attribute attribute = holder.attribute;
		Class type = attribute.getJavaType();
		if (node.getArguments().size() > 1) {
			/**
			 * Implementasi List
			 */
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
			/**
			 * Searching With One Value
			 */
			Object argument = castDynamicClass(type, node.getArguments().get(0));
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
				throw new IllegalArgumentException(String.format(
						"Operator %s can be used only for Comparables", op));
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
		throw new IllegalArgumentException("Unknown operator: " + op);
	}

	private Predicate[] processNodes(List<Node> nodes, Root root) {

		Predicate[] predicates = new Predicate[nodes.size()];

		for (int i = 0; i < nodes.size(); i++) {
			predicates[i] = nodes.get(i).accept(this, root);
		}
		return predicates;
	}

	public Object castDynamicClass(Class dynamicClass, String value) {
		Object object = null;
		try {
			if (dynamicClass.equals(Date.class) || dynamicClass.equals(java.sql.Date.class)) {
				object = java.sql.Date.valueOf(LocalDate.parse(value));
			} else if (dynamicClass.equals(LocalDate.class)) {
				object = LocalDate.parse(value);
			} else if (dynamicClass.equals(LocalDateTime.class)) {
				object = LocalDateTime.parse(value);
			} else if (dynamicClass.equals(Character.class)) {
				object = (!StringUtils.isEmpty(value) ? value.charAt(0) : null);
			} else if (dynamicClass.equals(boolean.class) || dynamicClass.equals(Boolean.class)) {
				object = Boolean.valueOf(value);
			} else {
				Constructor<?> cons = (Constructor<?>) dynamicClass.getConstructor(new Class<?>[] { String.class });
				object = cons.newInstance(new Object[] { value });
			}

			return object;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
