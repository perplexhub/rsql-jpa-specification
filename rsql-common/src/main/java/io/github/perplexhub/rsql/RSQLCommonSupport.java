package io.github.perplexhub.rsql;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.ManagedType;

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
	private @Getter static final Map<Class, Function<String, Object>> valueParserMap = new ConcurrentHashMap<>();
	private @Getter static final Map<Class, Class> valueTypeMap = new ConcurrentHashMap<>();
	private @Getter static final Map<Class<?>, List<String>> propertyWhitelist = new ConcurrentHashMap<>();
	private @Getter static final Map<Class<?>, List<String>> propertyBlacklist = new ConcurrentHashMap<>();

	public RSQLCommonSupport() {
	}

	public RSQLCommonSupport(Map<String, EntityManager> entityManagerMap) {
		if (entityManagerMap != null) {
			RSQLCommonSupport.entityManagerMap.putAll(entityManagerMap);
			log.info("{} EntityManager bean{} found: {}", entityManagerMap.size(), entityManagerMap.size() > 1 ? "s are" : " is", entityManagerMap);
		} else {
			log.warn("No EntityManager beans are found");
		}
		RSQLVisitorBase.setEntityManagerMap(getEntityManagerMap());
		RSQLVisitorBase.setManagedTypeMap(getManagedTypeMap());
		RSQLVisitorBase.setPropertyRemapping(getPropertyRemapping());
		RSQLVisitorBase.setValueParserMap(getValueParserMap());
		RSQLVisitorBase.setPropertyWhitelist(getPropertyWhitelist());
		RSQLVisitorBase.setPropertyBlacklist(getPropertyBlacklist());
		log.info("RSQLCommonSupport {}is initialized", getVersion());
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

	public static void addEntityAttributeParser(Class valueClass, Function<String, Object> function) {
		log.info("Adding entity attribute parser for {}", valueClass);
		if (valueClass != null && function != null) {
			valueParserMap.put(valueClass, function);
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
