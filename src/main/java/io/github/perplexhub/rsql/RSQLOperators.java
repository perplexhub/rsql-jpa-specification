package io.github.perplexhub.rsql;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;

public class RSQLOperators {

	public static final ComparisonOperator EQUAL = new ComparisonOperator("=="),
			NOT_EQUAL = new ComparisonOperator("!="),
			GREATER_THAN = new ComparisonOperator("=gt=", ">"),
			GREATER_THAN_OR_EQUAL = new ComparisonOperator("=ge=", ">="),
			LESS_THAN = new ComparisonOperator("=lt=", "<"),
			LESS_THAN_OR_EQUAL = new ComparisonOperator("=le=", "<="),
			IN = new ComparisonOperator("=in=", true),
			NOT_IN = new ComparisonOperator("=out=", true),
			IS_NULL = new ComparisonOperator("=na=", "=isnull=", "=null="),
			NOT_NULL = new ComparisonOperator("=nn=", "=notnull=", "=isnotnull="),
			LIKE = new ComparisonOperator("=ke=", "=like="),
			NOT_LIKE = new ComparisonOperator("=nk=", "=notlike="),
			IGNORE_CASE = new ComparisonOperator("=ic=", "=icase="),
			IGNORE_CASE_LIKE = new ComparisonOperator("=ik=", "=ilike="),
			IGNORE_CASE_NOT_LIKE = new ComparisonOperator("=ni=", "=inotlike="),
			BETWEEN = new ComparisonOperator("=bt=", "=between=", true),
			NOT_BETWEEN = new ComparisonOperator("=nb=", "=notbetween=", true);

	public static Set<ComparisonOperator> supportedOperators() {
		return new HashSet<>(Arrays.asList(EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL,
				LESS_THAN, LESS_THAN_OR_EQUAL, IN, NOT_IN, IS_NULL, NOT_NULL,
				LIKE, NOT_LIKE, IGNORE_CASE, IGNORE_CASE_LIKE, IGNORE_CASE_NOT_LIKE,
				BETWEEN, NOT_BETWEEN));
	}

}
