package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.perplexhub.rsql.jsonb.JsonbSupport;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.criteria.JpaExpression;

@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RSQLJPAPredicateConverter extends RSQLVisitorBase<Predicate, From> {

	private final CriteriaBuilder builder;
	private final Map<String, Path> cachedJoins = new HashMap<>();
	private final @Getter Map<String, String> propertyPathMapper;
	private final @Getter Map<ComparisonOperator, RSQLCustomPredicate<?>> customPredicates;
	private final @Getter Map<String, JoinType> joinHints;
	private final Collection<String> procedureWhiteList;
	private final Collection<String> procedureBlackList;
	private final boolean strictEquality;
	private final Character likeEscapeCharacter;

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper) {
		this(builder, propertyPathMapper, null, null);
	}

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper, List<RSQLCustomPredicate<?>> customPredicates) {
		this(builder, propertyPathMapper, customPredicates, null);
	}

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper, List<RSQLCustomPredicate<?>> customPredicates, Map<String, JoinType> joinHints) {
		this(builder, propertyPathMapper, customPredicates, joinHints, null, null, false, null);
	}

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper, List<RSQLCustomPredicate<?>> customPredicates, Map<String, JoinType> joinHints, Collection<String> procedureWhiteList, Collection<String> procedureBlackList) {
		this(builder, propertyPathMapper, customPredicates, joinHints, procedureWhiteList, procedureBlackList, false, null);
	}

	public RSQLJPAPredicateConverter(CriteriaBuilder builder,
			Map<String, String> propertyPathMapper,
			List<RSQLCustomPredicate<?>> customPredicates,
			Map<String, JoinType> joinHints,
			Collection<String> proceduresWhiteList,
			Collection<String> proceduresBlackList,
			boolean strictEquality,
			Character likeEscapeCharacter) {
		this.builder = builder;
		this.propertyPathMapper = propertyPathMapper != null ? propertyPathMapper : Collections.emptyMap();
		this.customPredicates = customPredicates != null ? customPredicates.stream().collect(Collectors.toMap(RSQLCustomPredicate::getOperator, Function.identity(), (a, b) -> a)) : Collections.emptyMap();
		this.joinHints = joinHints != null ? joinHints : Collections.emptyMap();
		this.procedureWhiteList = proceduresWhiteList != null ? proceduresWhiteList : Collections.emptyList();
		this.procedureBlackList = proceduresBlackList != null ? proceduresBlackList : Collections.emptyList();
		this.strictEquality = strictEquality;
		this.likeEscapeCharacter = likeEscapeCharacter;
	}

	RSQLJPAContext findPropertyPath(String propertyPath, Path startRoot) {
        return findPropertyPathInternal(propertyPath, startRoot, true);
	}

	private RSQLJPAContext findPropertyPathInternal(String propertyPath, Path startRoot, boolean firstTry) {
		Class type = startRoot.getJavaType();
		ManagedType<?> classMetadata = getManagedType(type);
		ManagedType<?> previousClassMetadata = null;
		Path<?> root = startRoot;
		Attribute<?, ?> attribute = null;
		String resolvedPropertyPath = firstTry? mapPropertyPath(propertyPath) : propertyPath;
		String[] properties = mapPropertyPath(resolvedPropertyPath).split("\\.");
		for (int i = 0, propertiesLength = properties.length; i < propertiesLength; i++) {
			String property = properties[i];
			String mappedProperty = mapProperty(property, classMetadata.getJavaType());
			if (!mappedProperty.equals(property)) {
				RSQLJPAContext context = findPropertyPathInternal(mappedProperty, root, firstTry);
				root = context.getPath();
				attribute = context.getAttribute();
				classMetadata = context.getManagedType();
			} else {
				if (!hasPropertyName(mappedProperty, classMetadata)) {
					Optional<String> mayBeJSonPath = PathUtils
							.findMappingOnBeginning(propertyPath, propertyPathMapper);
					//firstTry check to avoid stack overflow on cyclic mapping
					if(firstTry && mayBeJSonPath.isPresent()) {
						//Try with path mapping that matches just the beginning of the expression if json
						return findPropertyPathInternal(mayBeJSonPath.get(), startRoot, false);
					}
					throw new UnknownPropertyException(mappedProperty, classMetadata.getJavaType());
				}
				if (isAssociationType(mappedProperty, classMetadata) && !property.equals(resolvedPropertyPath)) {
					boolean isOneToAssociationType = isOneToOneAssociationType(mappedProperty, classMetadata) || isOneToManyAssociationType(mappedProperty, classMetadata);
					Class<?> associationType = findPropertyType(mappedProperty, classMetadata);
					type = associationType;
					String previousClass = classMetadata.getJavaType().getName();
					previousClassMetadata = classMetadata;
					classMetadata = getManagedType(associationType);

					String keyJoin = getKeyJoin(root, mappedProperty);
				if (isOneToAssociationType) {
					if (joinHints.containsKey(keyJoin)) {
						log.debug("Create a join between [{}] and [{}] using key [{}] with supplied hints", previousClass, classMetadata.getJavaType().getName(), keyJoin);
						root = join(keyJoin, root, mappedProperty, joinHints.get(keyJoin));
					} else {
						log.debug("Create a join between [{}] and [{}] using key [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
						root = join(keyJoin, root, mappedProperty, JoinType.LEFT);
					}
				} else {
					String lookAheadProperty = i < propertiesLength - 1 ? properties[i + 1] : null;
					boolean lookAheadPropertyIsId = false;
					if (!isManyToManyAssociationType(mappedProperty, previousClassMetadata) && classMetadata instanceof IdentifiableType && lookAheadProperty != null) {
						final IdentifiableType identifiableType = (IdentifiableType) classMetadata;
						final SingularAttribute id = identifiableType.getId(identifiableType.getIdType().getJavaType());
						if (identifiableType.hasSingleIdAttribute() && id.isId() && id.getName().equals(lookAheadProperty)) {
							lookAheadPropertyIsId = true;
						}
					}
					if (lookAheadPropertyIsId || lookAheadProperty == null) {
						log.debug("Create property path for type [{}] property [{}]", classMetadata.getJavaType().getName(), mappedProperty);
						root = root.get(mappedProperty);
					} else {
						log.debug("Create a join between [{}] and [{}] using key [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
						root = join(keyJoin, root, mappedProperty, joinHints.get(keyJoin));
					}
			  	}
			} else if (isElementCollectionType(mappedProperty, classMetadata)) {
					String previousClass = classMetadata.getJavaType().getName();
					attribute = RSQLVisitorBase.getAttribute(property, classMetadata);
					classMetadata = getManagedElementCollectionType(mappedProperty, classMetadata);
					String keyJoin = getKeyJoin(root, mappedProperty);
					log.debug("Create a element collection join between [{}] and [{}] using key [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
					root = join(keyJoin, root, mappedProperty);
				} else if (JsonbSupport.isJsonType(mappedProperty, classMetadata)) {
					root = root.get(mappedProperty);
					attribute = RSQLVisitorBase.getAttribute(mappedProperty, classMetadata);
					break;
				} else {
					log.debug("Create property path for type [{}] property [{}]", classMetadata.getJavaType().getName(), mappedProperty);
					root = root.get(mappedProperty);

					if (isEmbeddedType(mappedProperty, classMetadata)) {
						Class<?> embeddedType = findPropertyType(mappedProperty, classMetadata);
						type = embeddedType;
						classMetadata = getManagedType(embeddedType);
					} else {
						attribute = RSQLVisitorBase.getAttribute(property, classMetadata);
					}
				}
			}
		}

		if (attribute != null) {
			accessControl(type, attribute.getName());
		}

		return RSQLJPAContext.of(root, attribute, classMetadata);
	}

	private String getKeyJoin(Path<?> root, String mappedProperty) {
		return root.getJavaType().getSimpleName().concat(".").concat(mappedProperty);
	}

	protected Path<?> join(String keyJoin, Path<?> root, String mappedProperty) {
		return join(keyJoin, root, mappedProperty, null);
	}

	protected Path<?> join(String keyJoin, Path<?> root, String mappedProperty, JoinType joinType) {
		log.debug("join(keyJoin:{},root:{},mappedProperty:{},joinType:{})", keyJoin, root, mappedProperty, joinType);

		if (cachedJoins.containsKey(keyJoin)) {
			root = cachedJoins.get(keyJoin);
		} else {
			root = JoinUtils.getOrCreateJoin((From) root, mappedProperty, joinType);
			cachedJoins.put(keyJoin, root);
		}
		return root;
	}

	@Override
	public Predicate visit(ComparisonNode node, From root) {
		log.debug("visit(node:{},root:{})", node, root);

		ComparisonOperator op = node.getOperator();
		if (customPredicates.containsKey(op)) {
			RSQLCustomPredicate<?> customPredicate = customPredicates.get(op);
			List<Object> arguments = new ArrayList<>();
			for (String argument : node.getArguments()) {
				arguments.add(convert(argument, customPredicate.getType()));
			}
			RSQLJPAContext holder = findPropertyPath(node.getSelector(), root);
			return customPredicate.getConverter().apply(RSQLCustomPredicateInput.of(builder, holder.getPath(),
				holder.getAttribute(), arguments, root));
		}

		Selector selector = Selector.selectorOf(node.getSelector(), builder);
		Selector.assertWhiteListed(selector, procedureWhiteList);
		Selector.assertNotBlackListed(selector, procedureBlackList);

		var resolvedExpression = resolveExpression(node, root, selector);
		log.debug("Resolved expression: {}", resolvedExpression);
		//TODO: Use pattern matching when available
		if(resolvedExpression instanceof ResolvedExpression.JsonbPathExpression jsonbPathExpression) {
			return jsonPredicate(jsonbPathExpression);
		} else if (resolvedExpression instanceof ResolvedExpression.PathExpression pathExpression) {
			return expressionPredicate(node, pathExpression);
		} else {
			throw new IllegalArgumentException("Unknown resolved expression type: " + resolvedExpression.getClass());
		}
	}

	/**
	 * Get the resolved expression for the given node<br>
	 * If the node points to a jsonb attribute, it will return a {@link ResolvedExpression.JsonbPathExpression}<br>
	 * If the node points to a regular attribute or a function, it will return a {@link ResolvedExpression.PathExpression}
	 * @param node The node to resolve
	 * @param root The root of the query
	 * @param selector The selector of the node
	 * @return The resolved expression
	 */
	private ResolvedExpression resolveExpression(ComparisonNode node, From root, Selector selector) {
		//TODO: Use pattern matching when available
		if(selector instanceof Selector.SingleColumnSelector singleColumnSelector) {
			var holder = findPropertyPath(singleColumnSelector.column(), root);
			var attribute = holder.getAttribute();
			var path = holder.getPath();
			var type = path.getJavaType() != null ? path.getJavaType() : attribute.getJavaType();
			if(JsonbSupport.isJsonType(attribute)) {
				String jsonSelector = PathUtils.expectBestMapping(node.getSelector(), propertyPathMapper);
				String jsonbPath = JsonbSupport.jsonPathOfSelector(attribute, jsonSelector);
				if(jsonbPath.contains(".")) {
					ComparisonNode jsonbNode = node.withSelector(jsonbPath);
					return JsonbSupport.jsonbPathExistsExpression(builder, jsonbNode, path);
				} else {
					final Expression expression;
					if (path instanceof JpaExpression jpaExpression) {
						expression = jpaExpression.cast(String.class);
					} else {
						expression = path.as(String.class);
					}
					return ResolvedExpression.ofPath(expression, String.class);
				}
			} else {
				if (attribute != null
					&& attribute.getPersistentAttributeType() == PersistentAttributeType.ELEMENT_COLLECTION) {
					type = getElementCollectionGenericType(type, attribute);
				}
				if (type.isPrimitive()) {
					type = primitiveToWrapper.get(type);
				} else if (RSQLJPASupport.getValueTypeMap().containsKey(type)) {
					type = RSQLJPASupport.getValueTypeMap().get(type); // if you want to treat Enum as String and apply like search, etc
				}
				return ResolvedExpression.ofPath(holder.getPath(), type);
			}

		} else if(selector instanceof Selector.FunctionSelector) {
			var expression = selector.getExpression((column, criteriaBuilder) -> findPropertyPath(column, root).getPath());
			return ResolvedExpression.ofPath(expression, Object.class);
		} else {
			throw new IllegalArgumentException("Unknown selector type: " + selector.getClass());
		}
	}

	/**
	 * Transform the given JsonbPathExpression into a {@link Predicate}
	 *
	 * @param jsonbPathExpression The JsonbPathExpression to transform
	 * @return The Predicate
	 */
	private Predicate jsonPredicate(ResolvedExpression.JsonbPathExpression jsonbPathExpression) {
		if (jsonbPathExpression.inverted()) {
			return builder.isFalse(jsonbPathExpression.expression());
		} else {
			return builder.isTrue(jsonbPathExpression.expression());
		}
	}

	/**
	 * Transform the given PathExpression into a {@link Predicate}
	 *
	 * @param node The node to transform
	 * @param resolvedExpression The resolved expression
	 * @return The Predicate
	 */
	private Predicate expressionPredicate(ComparisonNode node, ResolvedExpression.PathExpression resolvedExpression) {
		Expression expression = resolvedExpression.expression();
		Class type = resolvedExpression.type();
		var op = node.getOperator();
		var arguments = node.getArguments();
		if (arguments.size() > 1) {
			List<Object> listObject = new ArrayList<>();
			for (String argument : arguments) {
				listObject.add(convert(argument, type));
			}
			if (op.equals(IN)) {
				return expression.in(listObject);
			}
			if (op.equals(NOT_IN)) {
				return expression.in(listObject).not();
			}
			if (op.equals(BETWEEN)
				&& listObject.get(0) instanceof Comparable comp1
				&& listObject.get(1) instanceof Comparable comp2) {
				return builder.between(expression, comp1, comp2);
			}
			if (op.equals(NOT_BETWEEN) &&
				listObject.get(0) instanceof Comparable comp1
				&& listObject.get(1) instanceof Comparable comp2) {
				return builder.between(expression, comp1, comp2).not();
			}
		} else {

			if (op.equals(IS_NULL)) {
				return builder.isNull(expression);
			}
			if (op.equals(NOT_NULL)) {
				return builder.isNotNull(expression);
			}
			Object argument = convert(arguments.get(0), type);
			if (op.equals(IN)) {
				return builder.equal(expression, argument);
			}
			if (op.equals(NOT_IN)) {
				return builder.notEqual(expression, argument);
			}
			if (op.equals(LIKE)) {
				return likePredicate(expression.as(String.class), "%" + argument.toString() + "%", builder);
			}
			if (op.equals(NOT_LIKE)) {
				return likePredicate(expression.as(String.class), "%" + argument.toString() + "%", builder).not();
			}
			if (op.equals(IGNORE_CASE)) {
				return builder.equal(builder.upper(expression), argument.toString().toUpperCase());
			}
			if (op.equals(IGNORE_CASE_LIKE)) {
				return likePredicate(builder.upper(expression), "%" + argument.toString()
						.toUpperCase() + "%", builder);
			}
			if (op.equals(IGNORE_CASE_NOT_LIKE)) {
				return likePredicate(builder.upper(expression), "%" + argument.toString()
						.toUpperCase() + "%", builder).not();
			}
			if (op.equals(EQUAL)) {
				return equalPredicate(expression, type, argument);
			}
			if (op.equals(NOT_EQUAL)) {
				return equalPredicate(expression, type, argument).not();
			}
			if (!Comparable.class.isAssignableFrom(type)) {
				log.error("Operator {} can be used only for Comparables", op);
				throw new RSQLException(String.format("Operator %s can be used only for Comparables", op));
			}
			Comparable comparable = (Comparable) argument;

			if (op.equals(GREATER_THAN)) {
				return builder.greaterThan(expression, comparable);
			}
			if (op.equals(GREATER_THAN_OR_EQUAL)) {
				return builder.greaterThanOrEqualTo(expression, comparable);
			}
			if (op.equals(LESS_THAN)) {
				return builder.lessThan(expression, comparable);
			}
			if (op.equals(LESS_THAN_OR_EQUAL)) {
				return builder.lessThanOrEqualTo(expression, comparable);
			}
		}
		log.error("Unknown operator: {}", op);
		throw new RSQLException("Unknown operator: " + op);
	}

	/**
	 * Convert a like expression to a like predicate
	 *
	 * @param attributePath The attribute path
     * @param likeExpression The like expression
	 * @param builder The criteria builder
	 */
	private Predicate likePredicate(Expression attributePath, String likeExpression, CriteriaBuilder builder) {
		return Optional.ofNullable(this.likeEscapeCharacter)
				.map(character ->  builder.like(attributePath, likeExpression, character))
				.orElseGet(() -> builder.like(attributePath, likeExpression));
	}

	private Predicate equalPredicate(Expression expr, Class type, Object argument) {
		if (type.equals(String.class)) {
			String argStr = argument.toString();

			if (strictEquality) {
				return builder.equal(expr, argument);
			} else {
				if (argStr.contains("*") && argStr.contains("^")) {
					return likePredicate(builder.upper(expr),
							argStr.replace('*', '%').replace("^", "").toUpperCase(),
							builder);
				} else if (argStr.contains("*")) {
					return likePredicate(expr, argStr.replace('*', '%'), builder);
				} else if (argStr.contains("^")) {
					return builder.equal(builder.upper(expr), argStr.replace("^", "").toUpperCase());
				} else {
					return builder.equal(expr, argument);
				}
			}
		} else if (argument == null) {
			return builder.isNull(expr);
		} else {
			return builder.equal(expr, argument);
		}
	}

	@Override
	public Predicate visit(AndNode node, From root) {
		log.debug("visit(node:{},root:{})", node, root);

		return node.getChildren().stream().map(n -> n.accept(this, root)).collect(Collectors.reducing(builder::and)).get();
	}

	@Override
	public Predicate visit(OrNode node, From root) {
		log.debug("visit(node:{},root:{})", node, root);

		return node.getChildren().stream().map(n -> n.accept(this, root)).collect(Collectors.reducing(builder::or)).get();
	}
}
