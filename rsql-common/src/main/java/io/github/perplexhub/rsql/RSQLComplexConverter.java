package io.github.perplexhub.rsql;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RSQLComplexConverter extends RSQLVisitorBase<Void, Map<String, MultiValueMap<String, String>>> {

	public RSQLComplexConverter() {
		super();
	}

	@Override
	public Void visit(ComparisonNode node, Map<String, MultiValueMap<String, String>> map) {
		log.debug("visit(node:{},map:{})", node, map);
		String key = node.getSelector();
		ComparisonOperator operator = node.getOperator();
		MultiValueMap<String, String> operatorMap = map.computeIfAbsent(key, k -> CollectionUtils.toMultiValueMap(new HashMap<>()));
		for (String ops : operator.getSymbols()) {
			operatorMap.addAll(ops, node.getArguments());
		}
		return null;
	}

	@Override
	public Void visit(AndNode node, Map<String, MultiValueMap<String, String>> map) {
		log.debug("visit(node:{},map:{})", node, map);
		node.getChildren().forEach(n -> n.accept(this, map));
		return null;
	}

	@Override
	public Void visit(OrNode node, Map<String, MultiValueMap<String, String>> map) {
		log.debug("visit(node:{},map:{})", node, map);
		node.getChildren().forEach(n -> n.accept(this, map));
		return null;
	}

}
