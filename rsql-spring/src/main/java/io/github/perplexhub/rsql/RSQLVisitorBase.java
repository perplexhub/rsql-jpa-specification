package io.github.perplexhub.rsql;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

import org.springframework.util.StringUtils;

import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class RSQLVisitorBase<R, A> implements RSQLVisitor<R, A> {

	protected static final Map<Class, Class> primitiveToWrapper;
	protected static @Setter Map<String, String> propertyPathMapper;
	protected static @Setter Map<Class<?>, Map<String, String>> propertyRemapping;
	protected static @Setter Map<Class, Function<String, Object>> valueParserMap;

	public Map<String, String> getPropertyPathMapper() {
		return propertyPathMapper != null ? propertyPathMapper : Collections.emptyMap();
	}

	public Map<Class<?>, Map<String, String>> getPropertyRemapping() {
		return propertyRemapping != null ? propertyRemapping : Collections.emptyMap();
	}

	public Map<Class, Function<String, Object>> getValueParserMap() {
		return valueParserMap != null ? valueParserMap : Collections.emptyMap();
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

}
