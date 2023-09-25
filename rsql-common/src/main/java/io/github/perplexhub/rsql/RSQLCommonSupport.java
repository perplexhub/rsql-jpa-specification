package io.github.perplexhub.rsql;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.ManagedType;

import lombok.Setter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import cz.jirutka.rsql.parser.RSQLParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "rawtypes" })
public class RSQLCommonSupport {

	private @Getter static final Map<String, EntityManager> entityManagerMap = new ConcurrentHashMap<>();
	private @Getter static final Map<Class, ManagedType> managedTypeMap = new ConcurrentHashMap<>();
	private @Getter static final Map<Class<?>, Map<String, String>> propertyRemapping = new ConcurrentHashMap<>();
	private @Getter static final Map<Class, Class> valueTypeMap = new ConcurrentHashMap<>();
	private @Getter static final Map<Class<?>, List<String>> propertyWhitelist = new ConcurrentHashMap<>();
	private @Getter static final Map<Class<?>, List<String>> propertyBlacklist = new ConcurrentHashMap<>();
	private @Getter static final ConfigurableConversionService conversionService = new DefaultConversionService();

	public RSQLCommonSupport() {
		init();
	}

	public RSQLCommonSupport(Map<String, EntityManager> entityManagerMap) {
		if (entityManagerMap != null) {
			setEntityManagerMap(entityManagerMap);
			log.info("{} EntityManager bean{} found: {}", entityManagerMap.size(), entityManagerMap.size() > 1 ? "s are" : " is", entityManagerMap);
		} else {
			log.warn("No EntityManager beans are found");
		}
		init();
	}

	protected void init() {
		conversionService.removeConvertible(Object.class, Object.class);
		RSQLVisitorBase.setEntityManagerMap(getEntityManagerMap());
		RSQLVisitorBase.setManagedTypeMap(getManagedTypeMap());
		RSQLVisitorBase.setPropertyRemapping(getPropertyRemapping());
		RSQLVisitorBase.setGlobalPropertyWhitelist(getPropertyWhitelist());
		RSQLVisitorBase.setGlobalPropertyBlacklist(getPropertyBlacklist());
		RSQLVisitorBase.setDefaultConversionService(getConversionService());
		log.info("RSQLCommonSupport {} is initialized", getVersion());
	}

	/**
	 * As the entityManager is renewed by Spring, we need to clear the managedTypeMap cache.
	 */
	private static void setEntityManagerMap(Map<String, EntityManager> entityManagerMap) {
		RSQLCommonSupport.entityManagerMap.putAll(entityManagerMap);
		RSQLCommonSupport.managedTypeMap.clear();
	}

	public static void addConverter(Converter<?, ?> converter) {
		conversionService.addConverter(converter);
	}

	public static <T> void addConverter(Class<T> targetType, Converter<String, ? extends T> converter) {
		log.info("Adding entity converter for {}", targetType);
		conversionService.addConverter(String.class, targetType, converter);
	}

	public static <T> void removeConverter(Class<T> targetType) {
		log.info("Removing entity converter for {}", targetType);
		conversionService.removeConvertible(String.class, targetType);
	}

	public static void addPropertyWhitelist(Class<?> entityClass, List<String> propertyList) {
		propertyWhitelist.computeIfAbsent(entityClass, entityClazz -> new ArrayList<>()).addAll(propertyList);
	}

	public static void addPropertyWhitelist(Class<?> entityClass, String property) {
		propertyWhitelist.computeIfAbsent(entityClass, entityClazz -> new ArrayList<>()).add(property);
	}

	public static void addPropertyBlacklist(Class<?> entityClass, List<String> propertyList) {
		propertyBlacklist.computeIfAbsent(entityClass, entityClazz -> new ArrayList<>()).addAll(propertyList);
	}

	public static void addPropertyBlacklist(Class<?> entityClass, String property) {
		propertyBlacklist.computeIfAbsent(entityClass, entityClazz -> new ArrayList<>()).add(property);
	}

	public static MultiValueMap<String, String> toMultiValueMap(final String rsqlQuery) {
		log.debug("toMultiValueMap(rsqlQuery:{})", rsqlQuery);
		MultiValueMap<String, String> map = CollectionUtils.toMultiValueMap(new HashMap<>());
		if (StringUtils.hasText(rsqlQuery)) {
			new RSQLParser(RSQLOperators.supportedOperators()).parse(rsqlQuery).accept(new RSQLSimpleConverter(), map);
		}
		return map;
	}

	public static Map<String, MultiValueMap<String, String>> toComplexMultiValueMap(final String rsqlQuery) {
		log.debug("toComplexMultiValueMap(rsqlQuery:{})", rsqlQuery);
		Map<String, MultiValueMap<String, String>> map = new HashMap<>();
		if (StringUtils.hasText(rsqlQuery)) {
			new RSQLParser(RSQLOperators.supportedOperators()).parse(rsqlQuery).accept(new RSQLComplexConverter(), map);
		}
		return map;
	}

	public static void addMapping(Class<?> entityClass, Map<String, String> mapping) {
		log.info("Adding entity class mapping for {}", entityClass);
		propertyRemapping.put(entityClass, mapping);
	}

	public static void addMapping(Class<?> entityClass, String selector, String property) {
		log.info("Adding entity class mapping for {}, selector {} and property {}", entityClass, selector, property);
		propertyRemapping.computeIfAbsent(entityClass, entityClazz -> new ConcurrentHashMap<>()).put(selector, property);
	}

	public static <T> void addEntityAttributeParser(Class<T> valueClass, Function<String, ? extends T> function) {
		log.info("Adding entity attribute parser for {}", valueClass);
		if (valueClass != null && function != null) {
			addConverter(valueClass, s -> function.apply(s));
		}
	}

	public static void addEntityAttributeTypeMap(Class valueClass, Class mappedClass) {
		log.info("Adding entity attribute type map for {} -> {}", valueClass, mappedClass);
		if (valueClass != null && mappedClass != null) {
			valueTypeMap.put(valueClass, mappedClass);
		}
	}

	protected String getVersion() {
		try {
			Properties prop = new Properties();
			prop.load(getClass().getResourceAsStream("/META-INF/maven/io.github.perplexhub/rsql-common/pom.properties"));
			String version = prop.getProperty("version");
			return StringUtils.hasText(version) ? "[" + version + "] " : "";
		} catch (Exception e) {
			return "";
		}
	}

}
