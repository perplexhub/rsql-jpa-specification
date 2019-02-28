package io.github.perplexhub.rsql.jpa;

import static java.util.Arrays.*;

import java.util.HashSet;
import java.util.Set;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;

public class RsqlOperators {

	public static final ComparisonOperator EQUAL = new ComparisonOperator("=="),
			NOT_EQUAL = new ComparisonOperator("!="),
			GREATER_THAN = new ComparisonOperator("=gt=", ">"),
			GREATER_THAN_OR_EQUAL = new ComparisonOperator("=ge=", ">="),
			LESS_THAN = new ComparisonOperator("=lt=", "<"),
			LESS_THAN_OR_EQUAL = new ComparisonOperator("=le=", "<="),
			IN = new ComparisonOperator("=in=", true),
			NOT_IN = new ComparisonOperator("=out=", true),
			IS_NULL = new ComparisonOperator("=na=", "=isnull=", "=null="),
			NOT_NULL = new ComparisonOperator("=nn=", "=notnull=", "=isnotnull=");

	public static Set<ComparisonOperator> supportedOperators() {
		return new HashSet<>(asList(EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL,
				LESS_THAN, LESS_THAN_OR_EQUAL, IN, NOT_IN, IS_NULL, NOT_NULL));
	}
}
