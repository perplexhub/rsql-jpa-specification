package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.*;
import static io.github.perplexhub.rsql.jsonb.JsonbSupport.*;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.springframework.orm.jpa.vendor.Database;

@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RSQLJPAPredicateConverter extends RSQLVisitorBase<Predicate, From> {

	private static final Set<Database> JSON_SUPPORT = EnumSet.of(Database.POSTGRESQL);

	private final CriteriaBuilder builder;
	private final Map<String, Path> cachedJoins = new HashMap<>();
	private final @Getter Map<String, String> propertyPathMapper;
	private final @Getter Map<ComparisonOperator, RSQLCustomPredicate<?>> customPredicates;
	private final @Getter Map<String, JoinType> joinHints;
	private final boolean strictEquality;

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper) {
		this(builder, propertyPathMapper, null, null);
	}

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper, List<RSQLCustomPredicate<?>> customPredicates) {
		this(builder, propertyPathMapper, customPredicates, null);
	}

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper, List<RSQLCustomPredicate<?>> customPredicates, Map<String, JoinType> joinHints) {
		this(builder, propertyPathMapper, customPredicates, joinHints, false);
	}
	
	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper,
																	 List<RSQLCustomPredicate<?>> customPredicates,
																	 Map<String, JoinType> joinHints,
																	 boolean strictEquality) {
		this.builder = builder;
		this.propertyPathMapper = propertyPathMapper != null ? propertyPathMapper : Collections.emptyMap();
		this.customPredicates = customPredicates != null ? customPredicates.stream().collect(Collectors.toMap(RSQLCustomPredicate::getOperator, Function.identity(), (a, b) -> a)) : Collections.emptyMap();
		this.joinHints = joinHints != null ? joinHints : Collections.emptyMap();
		this.strictEquality = strictEquality;
	}

	RSQLJPAContext findPropertyPath(String propertyPath, Path startRoot) {
		Class type = startRoot.getJavaType();
		ManagedType<?> classMetadata = getManagedType(type);
		ManagedType<?> previousClassMetadata = null;
		Path<?> root = startRoot;
		Attribute<?, ?> attribute = null;

		String[] properties = mapPropertyPath(propertyPath).split("\\.");

		for (int i = 0, propertiesLength = properties.length; i < propertiesLength; i++) {
			String property = properties[i];
			String mappedProperty = mapProperty(property, classMetadata.getJavaType());
			if (!mappedProperty.equals(property)) {
				RSQLJPAContext context = findPropertyPath(mappedProperty, root);
				root = context.getPath();
				attribute = context.getAttribute();
			} else {
				if (!hasPropertyName(mappedProperty, classMetadata)) {
					throw new UnknownPropertyException(mappedProperty, classMetadata.getJavaType());
				}
				if (isAssociationType(mappedProperty, classMetadata) && !property.equals(propertyPath)) {
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
					attribute = classMetadata.getAttribute(property);
					classMetadata = getManagedElementCollectionType(mappedProperty, classMetadata);
					String keyJoin = getKeyJoin(root, mappedProperty);
					log.debug("Create a element collection join between [{}] and [{}] using key [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
					root = join(keyJoin, root, mappedProperty);
				} else if (isJsonType(mappedProperty, classMetadata)) {
					root = root.get(mappedProperty);
					attribute = classMetadata.getAttribute(mappedProperty);
					break;
				} else {
					log.debug("Create property path for type [{}] property [{}]", classMetadata.getJavaType().getName(), mappedProperty);
					root = root.get(mappedProperty);

					if (isEmbeddedType(mappedProperty, classMetadata)) {
						Class<?> embeddedType = findPropertyType(mappedProperty, classMetadata);
						type = embeddedType;
						classMetadata = getManagedType(embeddedType);
					} else {
						attribute = classMetadata.getAttribute(property);
					}
				}
			}
		}

		if (attribute != null) {
			accessControl(type, attribute.getName());
		}

		return RSQLJPAContext.of(root, attribute);
	}

	private boolean isJsonType(String mappedProperty, ManagedType<?> classMetadata) {
		return Optional.ofNullable(classMetadata.getAttribute(mappedProperty))
				.map(this::isJsonType)
				.orElse(false);
	}
	
	protected boolean isJsonType(Attribute<?, ?> attribute) {
    	return isJsonColumn(attribute) && getDatabase(attribute).map(JSON_SUPPORT::contains).orElse(false);
	}

	private boolean isJsonColumn(Attribute<?, ?> attribute) {
		return Optional.ofNullable(attribute)
				.filter(attr -> attr.getJavaMember() instanceof Field)
				.map(attr -> ((Field) attr.getJavaMember()))
				.map(field -> field.getAnnotation(Column.class))
				.map(Column::columnDefinition)
				.map("jsonb"::equalsIgnoreCase)
				.orElse(false);
	}

	private Optional<Database> getDatabase(Attribute<?, ?> attribute) {
		return getEntityManagerMap()
				.values()
				.stream()
				.filter(em -> em.getMetamodel().getManagedTypes().contains(attribute.getDeclaringType()))
				.findFirst()
				.map(em -> getEntityManagerDatabase().get(em));
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
		RSQLJPAContext holder = findPropertyPath(node.getSelector(), root);
		Path attrPath = holder.getPath();
		Attribute attribute = holder.getAttribute();

		if (customPredicates.containsKey(op)) {
			RSQLCustomPredicate<?> customPredicate = customPredicates.get(op);
			List<Object> arguments = new ArrayList<>();
			for (String argument : node.getArguments()) {
				arguments.add(convert(argument, customPredicate.getType()));
			}
			return customPredicate.getConverter().apply(RSQLCustomPredicateInput.of(builder, attrPath, attribute, arguments, root));
		}
		Expression resolvedExpression = attrPath;
		if(isJsonType(attribute)) {
			String jsonbPath = jsonPathOfSelector(attribute, node.getSelector());
			if(jsonbPath.contains(".")) {
				ComparisonNode jsonbNode = new ComparisonNode(node.getOperator(), jsonbPath, node.getArguments());
				return jsonbPathExists(builder, jsonbNode, attrPath);
			} else {
				resolvedExpression = attrPath.as(String.class);
			}
		}

		Class type = attribute != null ? attribute.getJavaType() : null;

		if (attribute != null) {
			if (attribute.getPersistentAttributeType() == PersistentAttributeType.ELEMENT_COLLECTION) {
				type = getElementCollectionGenericType(type, attribute);
			}
			if (type.isPrimitive()) {
				type = primitiveToWrapper.get(type);
			} else if (RSQLJPASupport.getValueTypeMap().containsKey(type)) {
				type = RSQLJPASupport.getValueTypeMap().get(type); // if you want to treat Enum as String and apply like search, etc
			}
		}

		if (type == null) {
			type = String.class;
		}

        if (node.getArguments().size() > 1) {
			List<Object> listObject = new ArrayList<>();
			for (String argument : node.getArguments()) {
				listObject.add(convert(argument, type));
			}
			if (op.equals(IN)) {
				return resolvedExpression.in(listObject);
			}
			if (op.equals(NOT_IN)) {
				return resolvedExpression.in(listObject).not();
			}
			if (op.equals(BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
				return builder.between(resolvedExpression, (Comparable) listObject.get(0), (Comparable) listObject.get(1));
			}
			if (op.equals(NOT_BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
				return builder.between(resolvedExpression, (Comparable) listObject.get(0), (Comparable) listObject.get(1)).not();
			}
		} else {

			if (op.equals(IS_NULL)) {
				return builder.isNull(resolvedExpression);
			}
			if (op.equals(NOT_NULL)) {
				return builder.isNotNull(resolvedExpression);
			}
			Object argument = convert(node.getArguments().get(0), type);
			if (op.equals(IN)) {
				return builder.equal(resolvedExpression, argument);
			}
			if (op.equals(NOT_IN)) {
				return builder.notEqual(resolvedExpression, argument);
			}
			if (op.equals(LIKE)) {
				return builder.like(resolvedExpression, "%" + argument.toString() + "%");
			}
			if (op.equals(NOT_LIKE)) {
				return builder.like(resolvedExpression, "%" + argument.toString() + "%").not();
			}
			if (op.equals(IGNORE_CASE)) {
				return builder.equal(builder.upper(resolvedExpression), argument.toString().toUpperCase());
			}
			if (op.equals(IGNORE_CASE_LIKE)) {
				return builder.like(builder.upper(resolvedExpression), "%" + argument.toString().toUpperCase() + "%");
			}
			if (op.equals(IGNORE_CASE_NOT_LIKE)) {
				return builder.like(builder.upper(resolvedExpression), "%" + argument.toString().toUpperCase() + "%").not();
			}
			if (op.equals(EQUAL)) {
				return equalPredicate(resolvedExpression, type, argument);
			}
			if (op.equals(NOT_EQUAL)) {
				return equalPredicate(resolvedExpression, type, argument).not();
			}
			if (!Comparable.class.isAssignableFrom(type)) {
				log.error("Operator {} can be used only for Comparables", op);
				throw new RSQLException(String.format("Operator %s can be used only for Comparables", op));
			}
			Comparable comparable = (Comparable) argument;

			if (op.equals(GREATER_THAN)) {
				return builder.greaterThan(resolvedExpression, comparable);
			}
			if (op.equals(GREATER_THAN_OR_EQUAL)) {
				return builder.greaterThanOrEqualTo(resolvedExpression, comparable);
			}
			if (op.equals(LESS_THAN)) {
				return builder.lessThan(resolvedExpression, comparable);
			}
			if (op.equals(LESS_THAN_OR_EQUAL)) {
				return builder.lessThanOrEqualTo(resolvedExpression, comparable);
			}
		}
		log.error("Unknown operator: {}", op);
		throw new RSQLException("Unknown operator: " + op);
	}

	/**
	 * Returns the jsonb path for the given attribute path and selector.<br>
	 * It extracts the jsonb part of the selector that can contains entity references before the jsonb path.
	 *
	 * @param attrPath the attribute path
	 * @param selector the selector
	 * @return the jsonb path
	 */
	protected static String jsonPathOfSelector(Attribute attrPath, String selector) {
		String attributeName = attrPath.getName();
		int attributePosition = selector.indexOf(attributeName);
		if(attributePosition < 0) {
			throw new IllegalArgumentException("The attribute name [" + attributeName + "] is not part of the selector [" + selector + "]");
		}
		return selector.substring(attributePosition + attributeName.length());
	}

	private Predicate equalPredicate(Expression expr, Class type, Object argument) {
		if (type.equals(String.class)) {
			String argStr = argument.toString();
			
			if (strictEquality) {
				return builder.equal(expr, argument); 
			} else {
				if (argStr.contains("*") && argStr.contains("^")) {
					return builder.like(builder.upper(expr), argStr.replace('*', '%').replace("^", "").toUpperCase());
				} else if (argStr.contains("*")) {
					return builder.like(expr, argStr.replace('*', '%'));
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
