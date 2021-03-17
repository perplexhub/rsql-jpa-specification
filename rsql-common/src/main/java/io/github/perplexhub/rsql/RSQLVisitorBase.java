package io.github.perplexhub.rsql;

import java.lang.reflect.*;
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

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.util.StringUtils;

import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class RSQLVisitorBase<R, A> implements RSQLVisitor<R, A> {

	protected static volatile @Setter Map<Class, ManagedType> managedTypeMap;
	protected static volatile @Setter Map<String, EntityManager> entityManagerMap;
	protected static final Map<Class, Class> primitiveToWrapper;
	protected static volatile @Setter Map<Class<?>, Map<String, String>> propertyRemapping;
	protected static volatile @Setter Map<Class<?>, List<String>> propertyWhitelist;
	protected static volatile @Setter Map<Class<?>, List<String>> propertyBlacklist;
	protected static volatile @Setter ConfigurableConversionService defaultConversionService;

	protected Map<Class, ManagedType> getManagedTypeMap() {
		return managedTypeMap != null ? managedTypeMap : Collections.emptyMap();
	}

	protected Map<String, EntityManager> getEntityManagerMap() {
		return entityManagerMap != null ? entityManagerMap : Collections.emptyMap();
	}

	protected abstract Map<String, String> getPropertyPathMapper();

	public Map<Class<?>, Map<String, String>> getPropertyRemapping() {
		return propertyRemapping != null ? propertyRemapping : Collections.emptyMap();
	}

	protected Object convert(String source, Class targetType) {
		log.debug("convert(source:{},targetType:{})", source, targetType);

		Object object = null;
		try {
			if (defaultConversionService.canConvert(String.class, targetType)) {
				object = defaultConversionService.convert(source, targetType);
			} else if (targetType.equals(String.class)) {
				object = source;
			} else if (targetType.equals(UUID.class)) {
				object = UUID.fromString(source);
			} else if (targetType.equals(Date.class) || targetType.equals(java.sql.Date.class)) {
				object = java.sql.Date.valueOf(LocalDate.parse(source));
			} else if (targetType.equals(LocalDate.class)) {
				object = LocalDate.parse(source);
			} else if (targetType.equals(LocalDateTime.class)) {
				object = LocalDateTime.parse(source);
			} else if (targetType.equals(OffsetDateTime.class)) {
				object = OffsetDateTime.parse(source);
			} else if (targetType.equals(ZonedDateTime.class)) {
				object = ZonedDateTime.parse(source);
			} else if (targetType.equals(Character.class)) {
				object = (!StringUtils.isEmpty(source) ? source.charAt(0) : null);
			} else if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
				object = Boolean.valueOf(source);
			} else if (targetType.isEnum()) {
				object = Enum.valueOf(targetType, source);
			} else {
				Constructor<?> cons = (Constructor<?>) targetType.getConstructor(new Class<?>[] { String.class });
				object = cons.newInstance(new Object[] { source });
			}

			return object;
		} catch (DateTimeParseException | IllegalArgumentException e) {
			log.debug("Parsing [{}] with [{}] causing [{}], skip", source, targetType.getName(), e.getMessage());
		} catch (Exception e) {
			log.error("Parsing [{}] with [{}] causing [{}], add your parser via RSQLSupport.addConverter(Type.class, Type::valueOf)", source, targetType.getName(), e.getMessage(), e);
		}
		return null;
	}

	protected void accessControl(Class type, String name) {
		log.debug("accessControl(type:{},name:{})", type, name);

		if (propertyWhitelist != null && propertyWhitelist.containsKey(type)) {
			if (!propertyWhitelist.get(type).contains(name)) {
				throw new IllegalArgumentException("Property " + type.getName() + "." + name + " is not on whitelist");
			}
		}

		if (propertyBlacklist != null && propertyBlacklist.containsKey(type)) {
			if (propertyBlacklist.get(type).contains(name)) {
				throw new IllegalArgumentException("Property " + type.getName() + "." + name + " is on blacklist");
			}
		}
	}

	protected String mapPropertyPath(String propertyPath) {
		if (!getPropertyPathMapper().isEmpty()) {
			String property = getPropertyPathMapper().get(propertyPath);
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
					log.debug("Found managed type [{}] in EntityManager [{}]", cls, entityManagerEntry.getKey());
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

	protected <T> ManagedType<T> getManagedElementCollectionType(String mappedProperty, ManagedType<T> classMetadata) {
		try {
			Class<?> cls = findPropertyType(mappedProperty, classMetadata);
			if (!cls.isPrimitive() && !primitiveToWrapper.containsValue(cls) && !cls.equals(String.class) && getEntityManagerMap().size() > 0) {
				ManagedType<T> managedType = getManagedTypeMap().get(cls);
				if (managedType != null) {
					log.debug("Found managed type [{}] in cache", cls);
					return managedType;
				}
				for (Entry<String, EntityManager> entityManagerEntry : getEntityManagerMap().entrySet()) {
					managedType = (ManagedType<T>) entityManagerEntry.getValue().getMetamodel().managedType(cls);
					getManagedTypeMap().put(cls, managedType);
					log.info("Found managed type [{}] in EntityManager [{}]", cls, entityManagerEntry.getKey());
					return managedType;
				}
			}
		} catch (Exception e) {
			log.warn("Unable to get the managed type of [{}]", mappedProperty, e);
		}
		return classMetadata;
	}

	protected <T> boolean hasPropertyName(String property, ManagedType<T> classMetadata) {
		Set<Attribute<? super T, ?>> names = classMetadata.getAttributes();
		for (Attribute<? super T, ?> name : names) {
			if (name.getName().equals(property))
				return true;
		}
		return false;
	}

	@SneakyThrows
	protected Class getElementCollectionGenericType(Class type, Attribute attribute) {
		Member member = attribute.getJavaMember();
		if (member instanceof Field) {
			Field field = (Field) member;
			Type genericType = field.getGenericType();
			if (genericType instanceof ParameterizedType) {
				ParameterizedType rawType = (ParameterizedType) genericType;
				Class elementCollectionClass = Class.forName(rawType.getActualTypeArguments()[0].getTypeName());
				log.info("Map element collection generic type [{}] to [{}]", attribute.getName(), elementCollectionClass);
				return elementCollectionClass;
			}
		}
		return type;
	}

	protected <T> boolean isEmbeddedType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).getPersistentAttributeType() == PersistentAttributeType.EMBEDDED;
	}

	protected <T> boolean isElementCollectionType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).getPersistentAttributeType() == PersistentAttributeType.ELEMENT_COLLECTION;
	}

	protected <T> boolean isAssociationType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).isAssociation();
	}

	protected <T> boolean isOneToOneAssociationType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).isAssociation()
				&& PersistentAttributeType.ONE_TO_ONE == classMetadata.getAttribute(property).getPersistentAttributeType();
	}

	protected <T> boolean isOneToManyAssociationType(String property, ManagedType<T> classMetadata) {
		return classMetadata.getAttribute(property).isAssociation()
				&& PersistentAttributeType.ONE_TO_MANY == classMetadata.getAttribute(property).getPersistentAttributeType();
	}

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

}
