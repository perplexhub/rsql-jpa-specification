package io.github.perplexhub.rsql;

import java.lang.reflect.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Map.Entry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;

import lombok.Getter;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.orm.jpa.vendor.Database;
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
	protected static volatile @Setter @Getter Map<EntityManager, Database> entityManagerDatabase = Map.of();
	protected static final Map<Class, Class> primitiveToWrapper;
	protected static volatile @Setter Map<Class<?>, Map<String, String>> propertyRemapping;
	protected static volatile @Setter Map<Class<?>, List<String>> globalPropertyWhitelist;
	protected static volatile @Setter Map<Class<?>, List<String>> globalPropertyBlacklist;
	protected static volatile @Setter ConfigurableConversionService defaultConversionService;

	protected @Setter Map<Class<?>, List<String>> propertyWhitelist;

	protected @Setter Map<Class<?>, List<String>> propertyBlacklist;

	protected Map<Class, ManagedType> getManagedTypeMap() {
		return managedTypeMap != null ? managedTypeMap : Collections.emptyMap();
	}

	public static Map<String, EntityManager> getEntityManagerMap() {
		return entityManagerMap != null ? entityManagerMap : Collections.emptyMap();
	}

	public static Database getDatabase(EntityManager entityManager) {
		return entityManagerDatabase.get(entityManager);
	}

	public static <T> Attribute<? super T, ?> getAttribute(String property, ManagedType<T> classMetadata) {
		// W/A found here: https://hibernate.atlassian.net/browse/HHH-18569
		// breaking change on hibernate side: https://github.com/hibernate/hibernate-orm/pull/6924#discussion_r1250474422
		if (classMetadata instanceof ManagedDomainType managedDomainType) {
			return managedDomainType.findSubTypesAttribute(property);
		}
		return classMetadata.getAttribute(property);
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
			} else if (targetType.equals(Timestamp.class)) {
				Date date = java.sql.Date.valueOf(LocalDate.parse(source));
				return new Timestamp(date.getTime());
			} else if (targetType.equals(LocalDate.class)) {
				object = LocalDate.parse(source);
			} else if (targetType.equals(LocalDateTime.class)) {
				object = LocalDateTime.parse(source);
			} else if (targetType.equals(OffsetDateTime.class)) {
				object = OffsetDateTime.parse(source);
			} else if (targetType.equals(ZonedDateTime.class)) {
				object = ZonedDateTime.parse(source);
			} else if (targetType.equals(Character.class)) {
				object = (!StringUtils.hasText(source) ? source.charAt(0) : null);
			} else if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
				object = Boolean.valueOf(source);
			} else if (targetType.isEnum()) {
				object = Enum.valueOf(targetType, source);
			} else if (targetType.equals(Instant.class)) {
				object = Instant.parse(source);
			} else {
				Constructor<?> cons = (Constructor<?>) targetType.getConstructor(new Class<?>[] { String.class });
				object = cons.newInstance(new Object[] { source });
			}

			return object;
		} catch (Exception ex) {
			if (targetType.equals(LocalDateTime.class)) {
				try {
					return ((LocalDate) convert(source, LocalDate.class)).atStartOfDay();
				} catch (Exception e) {
					ex.addSuppressed(e);
				}
			}

			log.debug("Parsing [{}] with [{}] causing [{}], add your parser via RSQLSupport.addConverter(Type.class, Type::valueOf)", source, targetType.getName(), ex.getMessage());
			throw new ConversionException(String.format("Failed to convert %s to %s type", source, targetType.getName()), ex);
		}
	}

	protected void accessControl(Class type, String name) {
		log.debug("accessControl(type:{},name:{})", type, name);

		if (propertyWhitelist != null && propertyWhitelist.containsKey(type)) {
			if (!propertyWhitelist.get(type).contains(name)) {
				String msg = "Property " + type.getName() + "." + name + " is not on whitelist";
				log.debug(msg);
				throw new PropertyNotWhitelistedException(name, type, msg);
			}
		} else if (globalPropertyWhitelist != null && globalPropertyWhitelist.containsKey(type)) {
			if (!globalPropertyWhitelist.get(type).contains(name)) {
				String msg = "Property " + type.getName() + "." + name + " is not on global whitelist";
				log.debug(msg);
				throw new PropertyNotWhitelistedException(name, type, msg);
			}
		}

		if (propertyBlacklist != null && propertyBlacklist.containsKey(type)) {
			if (propertyBlacklist.get(type).contains(name)) {
				String msg = "Property " + type.getName() + "." + name + " is on blacklist";
				log.debug(msg);
				throw new PropertyBlacklistedException(name, type, msg);
			}
		} else if (globalPropertyBlacklist != null && globalPropertyBlacklist.containsKey(type)) {
			if (globalPropertyBlacklist.get(type).contains(name)) {
				String msg = "Property " + type.getName() + "." + name + " is on global blacklist";
				log.debug(msg);
				throw new PropertyBlacklistedException(name, type, msg);
			}
		}
	}

	protected String mapPropertyPath(String propertyPath) {
		return PathUtils.findMappingOnWhole(propertyPath, getPropertyPathMapper())
				.orElse(propertyPath);
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
		if (getAttribute(property, classMetadata).isCollection()) {
			propertyType = ((PluralAttribute) getAttribute(property, classMetadata)).getBindableJavaType();
		} else {
			propertyType = getAttribute(property, classMetadata).getJavaType();
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
		try {
			return getAttribute(property, classMetadata) != null;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@SneakyThrows
	protected static Class getElementCollectionGenericType(Class type, Attribute attribute) {
		Member member = attribute.getJavaMember();
		if (member instanceof Field field) {
			Type genericType = field.getGenericType();
			if (genericType instanceof ParameterizedType rawType) {
				Class elementCollectionClass = Class.forName(rawType.getActualTypeArguments()[0].getTypeName());
				log.info("Map element collection generic type [{}] to [{}]", attribute.getName(), elementCollectionClass);
				return elementCollectionClass;
			}
		}
		return type;
	}

	protected <T> boolean isEmbeddedType(String property, ManagedType<T> classMetadata) {
		return getAttribute(property, classMetadata).getPersistentAttributeType() == PersistentAttributeType.EMBEDDED;
	}

	protected <T> boolean isElementCollectionType(String property, ManagedType<T> classMetadata) {
		return getAttribute(property, classMetadata).getPersistentAttributeType() == PersistentAttributeType.ELEMENT_COLLECTION;
	}

	protected <T> boolean isAssociationType(String property, ManagedType<T> classMetadata) {
		return getAttribute(property, classMetadata).isAssociation();
	}

	protected <T> boolean isOneToOneAssociationType(String property, ManagedType<T> classMetadata) {
		return getAttribute(property, classMetadata).isAssociation()
				&& PersistentAttributeType.ONE_TO_ONE == getAttribute(property, classMetadata).getPersistentAttributeType();
	}

	protected <T> boolean isOneToManyAssociationType(String property, ManagedType<T> classMetadata) {
		return getAttribute(property, classMetadata).isAssociation()
				&& PersistentAttributeType.ONE_TO_MANY == getAttribute(property, classMetadata).getPersistentAttributeType();
	}

	protected <T> boolean isManyToManyAssociationType(String property, ManagedType<T> classMetadata) {
		return getAttribute(property, classMetadata).isAssociation()
				&& PersistentAttributeType.MANY_TO_MANY == getAttribute(property, classMetadata).getPersistentAttributeType();
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
