package io.github.perplexhub.rsql;

import java.util.function.Function;

import jakarta.persistence.criteria.Predicate;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RSQLCustomPredicate<T extends Comparable<?>> {

	private ComparisonOperator operator;
	private Class<T> type;
	private Function<RSQLCustomPredicateInput, Predicate> converter;

}
