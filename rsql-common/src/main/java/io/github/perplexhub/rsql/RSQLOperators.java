package io.github.perplexhub.rsql;

import cz.jirutka.rsql.parser.ast.Arity;
import java.util.Arrays;
import java.util.Collections;
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
			IN = new ComparisonOperator("=in=", Arity.of(1, Integer.MAX_VALUE)),
			NOT_IN = new ComparisonOperator("=out=", Arity.of(1, Integer.MAX_VALUE)),
			IS_NULL = new ComparisonOperator(new String[]{"=na=", "=isnull=", "=null="}, Arity.of(0, 1)),
			NOT_NULL = new ComparisonOperator(new String[]{"=nn=", "=notnull=", "=isnotnull="}, Arity.of(0, 1)),
			LIKE = new ComparisonOperator("=ke=", "=like="),
			NOT_LIKE = new ComparisonOperator("=nk=", "=notlike="),
			IGNORE_CASE = new ComparisonOperator("=ic=", "=icase="),
			IGNORE_CASE_LIKE = new ComparisonOperator("=ik=", "=ilike="),
			IGNORE_CASE_NOT_LIKE = new ComparisonOperator("=ni=", "=inotlike="),
			BETWEEN = new ComparisonOperator("=bt=", "=between=", Arity.nary(2)),
			NOT_BETWEEN = new ComparisonOperator("=nb=", "=notbetween=", Arity.nary(2));

  private static final Set<ComparisonOperator> OPERATORS = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(EQUAL, NOT_EQUAL,
          GREATER_THAN, GREATER_THAN_OR_EQUAL,
          LESS_THAN, LESS_THAN_OR_EQUAL, IN, NOT_IN, IS_NULL, NOT_NULL,
          LIKE, NOT_LIKE, IGNORE_CASE, IGNORE_CASE_LIKE, IGNORE_CASE_NOT_LIKE,
          BETWEEN, NOT_BETWEEN))
  );

  public static Set<ComparisonOperator> supportedOperators() {
    return OPERATORS;
  }

}
