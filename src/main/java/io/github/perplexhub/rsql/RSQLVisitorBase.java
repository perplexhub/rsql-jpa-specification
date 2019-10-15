package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLSupport.*;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;

import org.springframework.util.StringUtils;

import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class RSQLVisitorBase<R, A> implements RSQLVisitor<R, A> {

	private @Setter Map<String, String> propertyPathMapper;
	static final Map<Class, Class> primitiveToWrapper;

	static {
		Map<Class, Class> map = new HashMap<>();
		map.put(boolean.class, Boolean.class);
		map.put(byte.class, Byte.class);
		map.put(char.class, Character.class);
		map.put(double.class, Double.class);
		map.put(float.class, Float.class);
		map.put(int.class, Integer.class);
		map.put(long.class, Long.class);
		map.put(short.class, Short.class);
		map.put(void.class, Void.class);
		primitiveToWrapper = Collections.unmodifiableMap(map);
	}

	protected Object castDynamicClass(Class dynamicClass, String value) {
		log.debug("castDynamicClass(dynamicClass:{},value:{})", dynamicClass, value);

		Object object = null;
		try {
			if (getValueParserMap().containsKey(dynamicClass)) {
				object = getValueParserMap().get(dynamicClass).apply(value);
			} else if (dynamicClass.equals(String.class)) {
				object = value;
			} else if (dynamicClass.equals(UUID.class)) {
				object = UUID.fromString(value);
			} else if (dynamicClass.equals(Date.class) || dynamicClass.equals(java.sql.Date.class)) {
				object = java.sql.Date.valueOf(LocalDate.parse(value));
			} else if (dynamicClass.equals(LocalDate.class)) {
				object = LocalDate.parse(value);
			} else if (dynamicClass.equals(LocalDateTime.class)) {
				object = LocalDateTime.parse(value);
			} else if (dynamicClass.equals(OffsetDateTime.class)) {
				object = OffsetDateTime.parse(value);
			} else if (dynamicClass.equals(ZonedDateTime.class)) {
				object = ZonedDateTime.parse(value);
			} else if (dynamicClass.equals(Character.class)) {
				object = (!StringUtils.isEmpty(value) ? value.charAt(0) : null);
			} else if (dynamicClass.equals(boolean.class) || dynamicClass.equals(Boolean.class)) {
				object = Boolean.valueOf(value);
			} else if (dynamicClass.isEnum()) {
				object = Enum.valueOf(dynamicClass, value);
			} else {
				Constructor<?> cons = (Constructor<?>) dynamicClass.getConstructor(new Class<?>[] { String.class });
				object = cons.newInstance(new Object[] { value });
			}

			return object;
		} catch (DateTimeParseException | IllegalArgumentException e) {
			log.debug("Parsing [{}] with [{}] causing [{}], skip", value, dynamicClass.getName(), e.getMessage());
		} catch (Exception e) {
			log.error("Parsing [{}] with [{}] causing [{}], add your value parser via RSQLSupport.addEntityAttributeParser(Type.class, Type::valueOf)", value, dynamicClass.getName(), e.getMessage(), e);
		}
		return null;
	}

	protected <T> Class<?> findPropertyType(String property, ManagedType<T> classMetadata) {
		Class<?> propertyType = null;
		if (classMetadata.getAttribute(property).isCollection()) {
			propertyType = ((PluralAttribute) classMetadata.getAttribute(property)).getBindableJavaType();
		} else {
			propertyType = classMetadata.getAttribute(property).getJavaType();
		}
		return propertyType;
	}

	@SneakyThrows(Exception.class)
	protected <T> ManagedType<T> getManagedType(Class<T> cls) {
		Exception ex = null;
		if (getEntityManagerMap().size() > 0) {
			ManagedType<T> managedType = getManagedTypeMap().get(cls);
			if (managedType != null) {
				log.debug("Found managed type [{}] in cache", cls);
				return managedType;
			}
			for (Entry<String, EntityManager> entityManagerEntry : getEntityManagerMap().entrySet()) {
				try {
					managedType = entityManagerEntry.getValue().getMetamodel().managedType(cls);
					getManagedTypeMap().put(cls, managedType);
					log.info("Found managed type [{}] in EntityManager [{}]", cls, entityManagerEntry.getKey());
					return managedType;
				} catch (Exception e) {
					if (e != null) {
						ex = e;
					}
					log.debug("[{}] not found in EntityManager [{}] due to [{}]", cls, entityManagerEntry.getKey(), e == null ? "-" : e.getMessage());
				}
			}
		}
		log.error("[{}] not found in EntityManager{}: [{}]", cls, getEntityManagerMap().size() > 1 ? "s" : "", StringUtils.collectionToCommaDelimitedString(getEntityManagerMap().keySet()));
		throw ex != null ? ex : new IllegalStateException("No entity manager bean found in application context");
	}

	protected String mapPropertyPath(String propertyPath) {
		if (propertyPathMapper != null && !propertyPathMapper.isEmpty()) {
			String property = propertyPathMapper.get(propertyPath);
			if (StringUtils.hasText(property)) {
				log.debug("Map propertyPath [{}] to [{}]", propertyPath, property);
				return property;
			}
		}
		return propertyPath;
	}

	protected String mapProperty(String selector, Class<?> entityClass) {
		if (!getPropertyRemapping().isEmpty()) {
			Map<String, String> map = getPropertyRemapping().get(entityClass);
			String property = (map != null) ? map.get(selector) : null;
			if (StringUtils.hasText(property)) {
				log.debug("Map property [{}] to [{}] for [{}]", selector, property, entityClass);
				return property;
			}
		}
		return selector;
	}

	protected <T> boolean hasPropertyName(String property, ManagedType<T> classMetadata) {
		Set<Attribute<? super T, ?>> names = classMetadata.getAttributes();
		for (Attribute<? super T, ?> name : names) {
			if (name.getName().equals(property))
				return true;
		}
		return false;
	}

	protected <T> boolean isEmbeddedType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).getPersistentAttributeType() == PersistentAttributeType.EMBEDDED;
	}

	protected <T> boolean isAssociationType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).isAssociation();
	}

}
