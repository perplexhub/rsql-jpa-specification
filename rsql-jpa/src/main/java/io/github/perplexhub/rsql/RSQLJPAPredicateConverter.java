package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLOperators.*;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RSQLJPAPredicateConverter extends RSQLVisitorBase<Predicate, From> {

	private final CriteriaBuilder builder;
	private final Map<String, Path> cachedJoins = new HashMap<>();
	private final @Getter Map<String, String> propertyPathMapper;
	private final @Getter Map<ComparisonOperator, RSQLCustomPredicate<?>> customPredicates;
	private final @Getter Map<String, JoinType> joinHints;

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper) {
		this(builder, propertyPathMapper, null, null);
	}

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper, List<RSQLCustomPredicate<?>> customPredicates) {
		this(builder, propertyPathMapper, customPredicates, null);
	}

	public RSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper, List<RSQLCustomPredicate<?>> customPredicates, Map<String, JoinType> joinHints) {
		super();
		this.builder = builder;
		this.propertyPathMapper = propertyPathMapper != null ? propertyPathMapper : Collections.emptyMap();
		this.customPredicates = customPredicates != null ? customPredicates.stream().collect(Collectors.toMap(RSQLCustomPredicate::getOperator, Function.identity(), (a, b) -> a)) : Collections.emptyMap();
		this.joinHints = joinHints != null ? joinHints : Collections.emptyMap();
	}

	<T> RSQLJPAContext findPropertyPath(String propertyPath, Path startRoot) {
		ManagedType<?> classMetadata = getManagedType(startRoot.getJavaType());
		Path<?> root = startRoot;
		Class type = startRoot.getJavaType();
		Attribute<?, ?> attribute = null;

		String[] properties = propertyPath.split("\\.");

		for (String property : properties) {
			String mappedProperty = mapProperty(property, classMetadata.getJavaType());
			if (!mappedProperty.equals(property)) {
				RSQLJPAContext context = findPropertyPath(mappedProperty, root);
				root = context.getPath();
				attribute = context.getAttribute();
			} else {
				if (!hasPropertyName(mappedProperty, classMetadata)) {
					if (Modifier.isAbstract(classMetadata.getJavaType().getModifiers())) {
						Optional<Class<?>> foundSubClass = (Optional<Class<?>>) new Reflections(classMetadata.getJavaType().getPackage().getName())
								.getSubTypesOf(classMetadata.getJavaType())
								.stream()
								.filter(subType -> hasPropertyName(mappedProperty, getManagedType(subType)))
								.findFirst();
						if (foundSubClass.isPresent()) {
							classMetadata = getManagedType(foundSubClass.get());
							root = root instanceof Join ? builder.treat((Join) root, foundSubClass.get()).get(property) : builder.treat((Path) root, foundSubClass.get()).get(property);
							attribute = classMetadata.getAttribute(property);
						} else {
							throw new IllegalArgumentException("Unknown property: " + mappedProperty + " from entity " + classMetadata.getJavaType().getName());
						}
					} else {
						throw new IllegalArgumentException("Unknown property: " + mappedProperty + " from entity " + classMetadata.getJavaType().getName());
					}
				} else {
					if (isAssociationType(mappedProperty, classMetadata) && !property.equals(propertyPath)) {
						boolean isOneToAssociationType = isOneToOneAssociationType(mappedProperty, classMetadata) || isOneToManyAssociationType(mappedProperty, classMetadata);
						Class<?> associationType = findPropertyType(mappedProperty, classMetadata);
						type = associationType;
						String previousClass = classMetadata.getJavaType().getName();
						classMetadata = getManagedType(associationType);

						String keyJoin = root.getJavaType().getSimpleName().concat(".").concat(mappedProperty);
						if (isOneToAssociationType) {
							if (joinHints.containsKey(keyJoin)) {
								log.debug("Create a join between [{}] and [{}] using key [{}] with supplied hints", previousClass, classMetadata.getJavaType().getName(), keyJoin);
								root = join(keyJoin, root, mappedProperty, joinHints.get(keyJoin));
							} else {
								log.debug("Create a join between [{}] and [{}] using key [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
								root = join(keyJoin, root, mappedProperty, JoinType.LEFT);
							}
						} else {
							log.debug("Create a join between [{}] and [{}] using key [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
							root = join(keyJoin, root, mappedProperty);
						}
					} else if (isElementCollectionType(mappedProperty, classMetadata)) {
						String previousClass = classMetadata.getJavaType().getName();
						attribute = classMetadata.getAttribute(property);
						classMetadata = getManagedElementCollectionType(mappedProperty, classMetadata);
						String keyJoin = root.getJavaType().getSimpleName().concat(".").concat(mappedProperty);
						log.debug("Create a element collection join between [{}] and [{}] using key [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
						root = join(keyJoin, root, mappedProperty);
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
		}

		if (attribute != null) {
			accessControl(type, attribute.getName());
		}

		return RSQLJPAContext.of(root, attribute);
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
			if (joinType == null) {
				root = ((From) root).join(mappedProperty);
			} else {
				root = ((From) root).join(mappedProperty, joinType);
			}
			cachedJoins.put(keyJoin, root);
		}
		return root;
	}

	@Override
	public Predicate visit(ComparisonNode node, From root) {
		log.debug("visit(node:{},root:{})", node, root);

		ComparisonOperator op = node.getOperator();
		RSQLJPAContext holder = findPropertyPath(mapPropertyPath(node.getSelector()), root);
		Path attrPath = holder.getPath();

		if (customPredicates.containsKey(op)) {
			RSQLCustomPredicate<?> customPredicate = customPredicates.get(op);
			List<Object> arguments = new ArrayList<>();
			for (String argument : node.getArguments()) {
				arguments.add(convert(argument, customPredicate.getType()));
			}
			return customPredicate.getConverter().apply(RSQLCustomPredicateInput.of(builder, attrPath, arguments, root));
		}

		Attribute attribute = holder.getAttribute();
		Class type = attribute.getJavaType();
		if (attribute.getPersistentAttributeType() == PersistentAttributeType.ELEMENT_COLLECTION) {
			type = getElementCollectionGenericType(type, attribute);
		}
		if (type.isPrimitive()) {
			type = primitiveToWrapper.get(type);
		} else if (RSQLJPASupport.getValueTypeMap().containsKey(type)) {
			type = RSQLJPASupport.getValueTypeMap().get(type); // if you want to treat Enum as String and apply like search, etc
		}

		if (node.getArguments().size() > 1) {
			List<Object> listObject = new ArrayList<>();
			for (String argument : node.getArguments()) {
				listObject.add(convert(argument, type));
			}
			if (op.equals(IN)) {
				return attrPath.in(listObject);
			}
			if (op.equals(NOT_IN)) {
				return attrPath.in(listObject).not();
			}
			if (op.equals(BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
				return builder.between(attrPath, (Comparable) listObject.get(0), (Comparable) listObject.get(1));
			}
			if (op.equals(NOT_BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
				return builder.between(attrPath, (Comparable) listObject.get(0), (Comparable) listObject.get(1)).not();
			}
		} else {
			if (op.equals(IS_NULL)) {
				return builder.isNull(attrPath);
			}
			if (op.equals(NOT_NULL)) {
				return builder.isNotNull(attrPath);
			}
			Object argument = convert(node.getArguments().get(0), type);
			if (op.equals(IN)) {
				return builder.equal(attrPath, argument);
			}
			if (op.equals(NOT_IN)) {
				return builder.notEqual(attrPath, argument);
			}
			if (op.equals(LIKE)) {
				return builder.like(attrPath, "%" + argument.toString() + "%");
			}
			if (op.equals(NOT_LIKE)) {
				return builder.like(attrPath, "%" + argument.toString() + "%").not();
			}
			if (op.equals(IGNORE_CASE)) {
				return builder.equal(builder.upper(attrPath), argument.toString().toUpperCase());
			}
			if (op.equals(IGNORE_CASE_LIKE)) {
				return builder.like(builder.upper(attrPath), "%" + argument.toString().toUpperCase() + "%");
			}
			if (op.equals(IGNORE_CASE_NOT_LIKE)) {
				return builder.like(builder.upper(attrPath), "%" + argument.toString().toUpperCase() + "%").not();
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
			Comparable comparable = (Comparable) argument;

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
