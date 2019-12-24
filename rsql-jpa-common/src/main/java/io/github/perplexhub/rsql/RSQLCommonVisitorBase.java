package io.github.perplexhub.rsql;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;

import org.springframework.util.StringUtils;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class RSQLCommonVisitorBase<R, A> extends RSQLVisitorBase<R, A> {

	protected static @Setter Map<Class, ManagedType> managedTypeMap;
	protected static @Setter Map<String, EntityManager> entityManagerMap;

	protected <T> Class<?> findPropertyType(String property, ManagedType<T> classMetadata) {
		Class<?> propertyType = null;
		if (classMetadata.getAttribute(property).isCollection()) {
			propertyType = ((PluralAttribute) classMetadata.getAttribute(property)).getBindableJavaType();
		} else {
			propertyType = classMetadata.getAttribute(property).getJavaType();
		}
		return propertyType;
	}

	protected Map<Class, ManagedType> getManagedTypeMap() {
		return managedTypeMap != null ? managedTypeMap : Collections.emptyMap();
	}

	protected Map<String, EntityManager> getEntityManagerMap() {
		return entityManagerMap != null ? entityManagerMap : Collections.emptyMap();
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

}
