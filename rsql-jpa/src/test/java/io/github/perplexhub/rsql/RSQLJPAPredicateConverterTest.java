package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.IGNORE_CASE;
import static io.github.perplexhub.rsql.RSQLOperators.IGNORE_CASE_LIKE;
import static io.github.perplexhub.rsql.RSQLOperators.IGNORE_CASE_NOT_LIKE;
import static io.github.perplexhub.rsql.RSQLOperators.LIKE;
import static io.github.perplexhub.rsql.RSQLOperators.NOT_LIKE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Set;
import java.util.stream.Stream;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RSQLJPAPredicateConverterTest {

	private static final Set<ComparisonOperator> TEXTUAL_ARGUMENT_OPERATORS = Set.of(
			LIKE,
			NOT_LIKE,
			IGNORE_CASE,
			IGNORE_CASE_LIKE,
			IGNORE_CASE_NOT_LIKE
	);

	@ParameterizedTest
	@MethodSource("supportedOperators")
	void preservesTextualArgumentOnlyMatchesTextualArgumentOperators(ComparisonOperator operator, boolean expected) {
		assertThat(RSQLJPAPredicateConverter.preservesTextualArgument(operator), is(expected));
	}

	static Stream<Arguments> supportedOperators() {
		return RSQLOperators.supportedOperators().stream()
				.map(operator -> arguments(operator, TEXTUAL_ARGUMENT_OPERATORS.contains(operator)));
	}
}
