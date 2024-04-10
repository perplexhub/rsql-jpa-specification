package io.github.perplexhub.rsql;

import io.github.perplexhub.rsql.jsonb.JsonbSupport;
import io.github.perplexhub.rsql.model.EntityWithJsonb;
import io.github.perplexhub.rsql.model.JsonbEntity;
import io.github.perplexhub.rsql.model.PostgresJsonEntity;
import io.github.perplexhub.rsql.repository.jpa.postgres.EntityWithJsonbRepository;
import io.github.perplexhub.rsql.repository.jpa.postgres.JsonbEntityRepository;
import io.github.perplexhub.rsql.repository.jpa.postgres.PostgresJsonEntityRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
@ActiveProfiles("postgres")
class RSQLJPASupportPostgresJsonTest {

    @Autowired
    private PostgresJsonEntityRepository repository;

    @Autowired
    private EntityWithJsonbRepository entityWithJsonbRepository;

    @Autowired
    private JsonbEntityRepository jsonbEntityRepository;

    @BeforeEach
    void setup(@Autowired EntityManager em) {
        RSQLVisitorBase.setEntityManagerDatabase(Map.of(em, Database.POSTGRESQL));
        clear();
        JsonbSupport.DATE_TIME_SUPPORT = false;
    }

    @AfterEach
    void tearDown() {
        clear();
        RSQLVisitorBase.setEntityManagerDatabase(Map.of());
    }

    private void clear() {
        repository.deleteAll();
        repository.flush();
        entityWithJsonbRepository.deleteAll();
        entityWithJsonbRepository.flush();
    }

    @ParameterizedTest
    @MethodSource("data")
    void testJsonSearch(List<PostgresJsonEntity> entities, String rsql, List<PostgresJsonEntity> expected) {
        //given
        repository.saveAllAndFlush(entities);

        //when
        List<PostgresJsonEntity> result = repository.findAll(toSpecification(rsql));

        //then
        assertThat(result)
                .hasSameSizeAs(expected)
                .containsExactlyInAnyOrderElementsOf(expected);

        entities.forEach(e -> e.setId(null));
    }

    @ParameterizedTest
    @MethodSource("temporalData")
    void testJsonSearchOfTemporal(List<PostgresJsonEntity> entities, String rsql, List<PostgresJsonEntity> expected) {
        JsonbSupport.DATE_TIME_SUPPORT = true;
        //given
        repository.saveAllAndFlush(entities);

        //when
        List<PostgresJsonEntity> result = repository.findAll(toSpecification(rsql));

        //then
        assertThat(result)
                .hasSameSizeAs(expected)
                .containsExactlyInAnyOrderElementsOf(expected);

        entities.forEach(e -> e.setId(null));
    }

    @ParameterizedTest
    @MethodSource("sortData")
    void testJsonSort(List<PostgresJsonEntity> entities, String rsql, List<PostgresJsonEntity> expected) {
        //given
        repository.saveAllAndFlush(entities);

        //when
        List<PostgresJsonEntity> result = repository.findAll(RSQLJPASupport.toSort(rsql));

        //then
        assertThat(result)
                .hasSameSizeAs(expected)
                .containsExactlyElementsOf(expected);
        entities.forEach(e -> e.setId(null));
    }

    @ParameterizedTest
    @MethodSource("jsonRelation")
    void testJsonSearchOnRelation(List<JsonbEntity> jsonbEntities, String rsql, List<JsonbEntity> expected) {
        //given
        Collection<EntityWithJsonb> entitiesWithJsonb = jsonbEntityRepository.saveAllAndFlush(jsonbEntities).stream()
                .map(jsonbEntity -> EntityWithJsonb.builder().jsonb(jsonbEntity).build())
                .toList();
        entityWithJsonbRepository.saveAllAndFlush(entitiesWithJsonb);

        //when
        List<JsonbEntity> result = entityWithJsonbRepository.findAll(toSpecification(rsql)).stream()
                .map(EntityWithJsonb::getJsonb)
                .toList();

        //then
        assertThat(result)
                .hasSameSizeAs(expected)
                .containsExactlyInAnyOrderElementsOf(expected);
        result.forEach(e -> e.setId(null));
    }

    @ParameterizedTest
    @MethodSource("jsonMappedRelation")
    void testJsonSearchOnMappedRelation(List<JsonbEntity> jsonbEntities, String rsql, List<JsonbEntity> expected) {
        //given
        Collection<EntityWithJsonb> entitiesWithJsonb = jsonbEntityRepository.saveAllAndFlush(jsonbEntities).stream()
                .map(jsonbEntity -> EntityWithJsonb.builder().jsonb(jsonbEntity).build())
                .toList();
        entityWithJsonbRepository.saveAllAndFlush(entitiesWithJsonb);
        Map<String, String> pathMapping = new HashMap<>();
        pathMapping.put("jsonbRelation", "jsonb.data");
        pathMapping.put("jsonbRelationOfA", "jsonb.data.a");
        //when
        List<JsonbEntity> result = entityWithJsonbRepository.findAll(toSpecification(rsql, pathMapping)).stream()
                .map(EntityWithJsonb::getJsonb)
                .toList();

        //then
        assertThat(result)
                .hasSameSizeAs(expected)
                .containsExactlyInAnyOrderElementsOf(expected);
        result.forEach(e -> e.setId(null));
    }

    @ParameterizedTest
    @MethodSource("sortByRelation")
    void testJsonSortOnRelation(List<JsonbEntity> jsonbEntities, String rsql, List<JsonbEntity> expected) {
        JsonbSupport.DATE_TIME_SUPPORT = true;
        //given
        Collection<EntityWithJsonb> entitiesWithJsonb = jsonbEntityRepository.saveAllAndFlush(jsonbEntities).stream()
                .map(jsonbEntity -> EntityWithJsonb.builder().jsonb(jsonbEntity).build())
                .toList();
        entityWithJsonbRepository.saveAllAndFlush(entitiesWithJsonb);

        //when
        List<JsonbEntity> result = entityWithJsonbRepository.findAll(RSQLJPASupport.toSort(rsql)).stream()
                .map(EntityWithJsonb::getJsonb)
                .toList();

        //then
        assertThat(result)
                .hasSameSizeAs(expected)
                .containsExactlyInAnyOrderElementsOf(expected);
        result.forEach(e -> e.setId(null));
    }

    @ParameterizedTest
    @MethodSource("sortByMappedRelation")
    void testJsonSortOnMappedRelation(List<JsonbEntity> jsonbEntities, String rsql, List<JsonbEntity> expected) {
        JsonbSupport.DATE_TIME_SUPPORT = true;
        //given
        Collection<EntityWithJsonb> entitiesWithJsonb = jsonbEntityRepository.saveAllAndFlush(jsonbEntities).stream()
                .map(jsonbEntity -> EntityWithJsonb.builder().jsonb(jsonbEntity).build())
                .toList();
        entityWithJsonbRepository.saveAllAndFlush(entitiesWithJsonb);
        Map<String, String> pathMapping = new HashMap<>();
        pathMapping.put("jsonbRelation", "jsonb.data");
        pathMapping.put("jsonbRelationOfA", "jsonb.data.a");
        //when
        List<JsonbEntity> result = entityWithJsonbRepository.findAll(RSQLJPASupport.toSort(rsql, pathMapping)).stream()
                .map(EntityWithJsonb::getJsonb)
                .toList();

        //then
        assertThat(result)
                .hasSameSizeAs(expected)
                .containsExactlyInAnyOrderElementsOf(expected);
        result.forEach(e -> e.setId(null));
    }

    static Stream<Arguments> data() {
        return Stream.of(
                equalsData(),
                inData(),
                betweenData(),
                likeData(),
                gtLtData(),
                miscData(),
                textData(),
                booleanData(),
                numberData(),
                arrayData(),
                doesNotConvertBoolean(),
                doesNotConvertInteger(),
                nestedData(),
                null
        ).filter(Objects::nonNull).flatMap(s -> s);
    }

    static Stream<Arguments> temporalData() {
        return Stream.of(
                dateData(),
                timeData(),
                dateTimeWithTzData(),
                dateTimeWithoutTzData(),
                meltedTimeZone(),
                null
        ).filter(Objects::nonNull).flatMap(s -> s);
    }

    static Stream<Arguments> sortData() {
        return Stream.of(
                sortByText(),
                sortByNumber(),
                sortByNested(),
                sortByMixedData(),
                null
        ).filter(Objects::nonNull).flatMap(s -> s);
    }

    static Stream<Arguments> jsonRelation() {
        var e1 = JsonbEntity.builder().id(UUID.randomUUID()).data("1").build();
        var e2 = JsonbEntity.builder().id(UUID.randomUUID()).data("\"value\"").build();
        var e3 = JsonbEntity.builder().id(UUID.randomUUID()).data("true").build();
        var e4 = JsonbEntity.builder().id(UUID.randomUUID()).data("[1,2,3]").build();
        var e5 = JsonbEntity.builder().id(UUID.randomUUID()).data("null").build();
        var e6 = JsonbEntity.builder().id(UUID.randomUUID()).data("{}").build();
        var e7 = JsonbEntity.builder().id(UUID.randomUUID()).data("{\"a\":1}").build();
        var allCases = List.of(e1, e2, e3, e4, e5, e6, e7);
        return Stream.of(
                arguments(allCases, "jsonb.data==1", List.of(e1)),
                arguments(allCases, "jsonb.data=='\"value\"'", List.of(e2)),
                arguments(allCases, "jsonb.data==true", List.of(e3)),
                arguments(allCases, "jsonb.data=='[1, 2, 3]'", List.of(e4)),
                arguments(allCases, "jsonb.data=='null'", List.of(e5)),
                arguments(allCases, "jsonb.data=='{}'", List.of(e6)),
                arguments(allCases, "jsonb.data=='{\"a\": 1}'", List.of(e7)),
                arguments(allCases, "jsonb.data.a==1", List.of(e7)),
                null
        ).filter(Objects::nonNull);
    }

    static Stream<Arguments> jsonMappedRelation() {
        var e1 = JsonbEntity.builder().id(UUID.randomUUID()).data("1").build();
        var e2 = JsonbEntity.builder().id(UUID.randomUUID()).data("\"value\"").build();
        var e3 = JsonbEntity.builder().id(UUID.randomUUID()).data("true").build();
        var e4 = JsonbEntity.builder().id(UUID.randomUUID()).data("[1,2,3]").build();
        var e5 = JsonbEntity.builder().id(UUID.randomUUID()).data("null").build();
        var e6 = JsonbEntity.builder().id(UUID.randomUUID()).data("{}").build();
        var e7 = JsonbEntity.builder().id(UUID.randomUUID()).data("{\"a\":1}").build();
        var allCases = List.of(e1, e2, e3, e4, e5, e6, e7);
        return Stream.of(
                arguments(allCases, "jsonb.data==1", List.of(e1)),
                arguments(allCases, "jsonb.data.a==1", List.of(e7)),
                arguments(allCases, "jsonbRelation==1", List.of(e1)),
                arguments(allCases, "jsonbRelation=='\"value\"'", List.of(e2)),
                arguments(allCases, "jsonbRelation==true", List.of(e3)),
                arguments(allCases, "jsonbRelation=='[1, 2, 3]'", List.of(e4)),
                arguments(allCases, "jsonbRelation=='null'", List.of(e5)),
                arguments(allCases, "jsonbRelation=='{}'", List.of(e6)),
                arguments(allCases, "jsonbRelation=='{\"a\": 1}'", List.of(e7)),
                arguments(allCases, "jsonbRelation.a==1", List.of(e7)),
                arguments(allCases, "jsonbRelationOfA==1", List.of(e7)),
                null
        ).filter(Objects::nonNull);
    }

    static Stream<Arguments> equalsData() {
        var e1 = new PostgresJsonEntity(Map.of("a1", "b1"));
        var e2 = new PostgresJsonEntity(Map.of("a1", Map.of("a11", Map.of("a111", "b1"))));

        var e3 = new PostgresJsonEntity(e1);
        var e4 = new PostgresJsonEntity(e2);

        var e5 = new PostgresJsonEntity(Map.of("a", "b1"));
        var e6 = new PostgresJsonEntity(Map.of("a", "b2"));
        var e7 = new PostgresJsonEntity(Map.of("a", "c1"));

        return Stream.of(
                arguments(List.of(e1, e2), "properties.a1==b1", List.of(e1)),
                arguments(List.of(e1, e2), "properties.a1!=b1", List.of(e2)),
                arguments(List.of(e1, e2), "properties.a1=ic=B1", List.of(e1)),
                arguments(List.of(e1, e2), "properties.a1==b2", List.of()),
                arguments(List.of(e3, e4), "properties.a1.a11.a111==b1", List.of(e4)),
                arguments(List.of(e3, e4), "properties.a1.a11.a111==b2", List.of()),

                arguments(List.of(e5, e6, e7), "properties.a==b*", List.of(e5, e6)),
                arguments(List.of(e5, e6, e7), "properties.a==c*", List.of(e7)),
                arguments(List.of(e5, e6, e7), "properties.a==*1", List.of(e5, e7))
        );
    }

    static Stream<Arguments> inData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "b1"));
        var e2 = new PostgresJsonEntity(Map.of("a", "b2"));
        var e3 = new PostgresJsonEntity(Map.of("a", "c1"));
        var e4 = new PostgresJsonEntity(Map.of("a", "d1"));

        return Stream.of(
                arguments(List.of(e1, e2, e3, e4), "properties.a=in=(b1, c1)", List.of(e1, e3)),
                arguments(List.of(e1, e2, e3, e4), "properties.a=out=(b1, c1)", List.of(e2, e4)),
                arguments(List.of(e1, e2, e3, e4), "properties.a=in=(b1)", List.of(e1)),
                arguments(List.of(e1, e2, e3, e4), "properties.a=out=(b1)", List.of(e2, e3, e4))
        );
    }

    static Stream<Arguments> betweenData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "a"));
        var e2 = new PostgresJsonEntity(Map.of("a", "b"));
        var e3 = new PostgresJsonEntity(Map.of("a", "c"));
        var e4 = new PostgresJsonEntity(Map.of("a", "d"));

        return Stream.of(
                arguments(List.of(e1, e2, e3, e4), "properties.a=bt=(a, c)", List.of(e1, e2, e3)),
                arguments(List.of(e1, e2, e3, e4), "properties.a=nb=(b, d)", List.of(e1))
        );
    }

    static Stream<Arguments> likeData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "a b c"));
        var e2 = new PostgresJsonEntity(Map.of("a", "b c d"));
        var e3 = new PostgresJsonEntity(Map.of("a", "c d e"));

        return Stream.of(
                arguments(List.of(e1, e2, e3), "properties.a=ke='a b'", List.of(e1)),
                arguments(List.of(e1, e2, e3), "properties.a=ke='b c'", List.of(e1, e2)),
                arguments(List.of(e1, e2, e3), "properties.a=ke='c d'", List.of(e2, e3)),
                arguments(List.of(e1, e2, e3), "properties.a=ke='d e'", List.of(e3)),

                arguments(List.of(e1, e2, e3), "properties.a=ik='A B'", List.of(e1)),
                arguments(List.of(e1, e2, e3), "properties.a=ik='B C'", List.of(e1, e2)),
                arguments(List.of(e1, e2, e3), "properties.a=ik='C D'", List.of(e2, e3)),
                arguments(List.of(e1, e2, e3), "properties.a=ik='D E'", List.of(e3)),

                arguments(List.of(e1, e2, e3), "properties.a=nk='a b'", List.of(e2, e3)),
                arguments(List.of(e1, e2, e3), "properties.a=ni='A B'", List.of(e2, e3))
        );
    }

    static Stream<Arguments> gtLtData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "a"));
        var e2 = new PostgresJsonEntity(Map.of("a", "b"));
        var e3 = new PostgresJsonEntity(Map.of("a", "c"));
        var e4 = new PostgresJsonEntity(Map.of("a", "d"));

        return Stream.of(
                arguments(List.of(e1, e2, e3, e4), "properties.a>=a", List.of(e1, e2, e3, e4)),
                arguments(List.of(e1, e2, e3, e4), "properties.a>a", List.of(e2, e3, e4)),
                arguments(List.of(e1, e2, e3, e4), "properties.a<a", List.of()),
                arguments(List.of(e1, e2, e3, e4), "properties.a<=a", List.of(e1)),
                arguments(List.of(e1, e2, e3, e4), "properties.a<b", List.of(e1)),
                arguments(List.of(e1, e2, e3, e4), "properties.a<=d", List.of(e1, e2, e3, e4))
        );
    }

    static Stream<Arguments> miscData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "b1"));
        var e2 = new PostgresJsonEntity(Map.of("a", "b2"));
        var e3 = new PostgresJsonEntity(Map.of("b", "c1"));
        var e4 = new PostgresJsonEntity(Map.of("b", "d1"));

        return Stream.of(
                arguments(List.of(e1, e2, e3, e4), "properties.a=nn=''", List.of(e1, e2)),
                arguments(List.of(e1, e2, e3, e4), "properties.a=na=''", List.of(e3, e4)),

                arguments(List.of(e1, e2, e3, e4), "properties.b=nn=''", List.of(e3, e4)),
                arguments(List.of(e1, e2, e3, e4), "properties.b=na=''", List.of(e1, e2))
        );
    }

    private static Stream<Arguments> textData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "Lorem"));
        var e2 = new PostgresJsonEntity(Map.of("a", "Ipsum"));
        var e3 = new PostgresJsonEntity(Map.of("a", "Dolor"));
        var e4 = new PostgresJsonEntity(Map.of("a", "Sit"));
        var e5 = new PostgresJsonEntity(Map.of("a", "Amet"));
        var e6 = new PostgresJsonEntity(Map.of("a", "Consectetur adipiscing elit"));
        var allCases = List.of(e1, e2, e3, e4, e5, e6);
        return Stream.of(
                arguments(allCases, "properties.a=like=Lorem", List.of(e1)),
                arguments(allCases, "properties.a=like='Consectetur adipiscing elit'", List.of(e6)),
                arguments(allCases, "properties.a=notlike=Lorem", List.of(e2, e3, e4, e5, e6)),
                arguments(allCases, "properties.a=like=*t", List.of(e4, e5, e6)),
                arguments(allCases, "properties.a=like=*o*", List.of(e1, e3, e6)),
                arguments(allCases, "properties.a=icase=dolor", List.of(e3)),
                arguments(allCases, "properties.a=inotlike=dolor", List.of(e1, e2, e4, e5, e6)),
                null
        ).filter(Objects::nonNull);
    }


    private static Stream<Arguments> booleanData() {
        var e1 = new PostgresJsonEntity(Map.of("a", true));
        var e2 = new PostgresJsonEntity(Map.of("a", false));
        var e3 = new PostgresJsonEntity(Map.of("a", true));
        var e4 = new PostgresJsonEntity(Map.of("a", false));
        var e5 = new PostgresJsonEntity(nullMap("a"));
        var e6 = new PostgresJsonEntity(Map.of("b", "other"));
        var allCases = List.of(e1, e2, e3, e4, e5, e6);
        return Stream.of(
                arguments(allCases, "properties.a==true", List.of(e1, e3)),
                arguments(allCases, "properties.a==false", List.of(e2, e4)),
                arguments(allCases, "properties.a!=true", List.of(e2, e4, e5, e6)),
                arguments(allCases, "properties.a!=false", List.of(e1, e3, e5, e6)),
                null
        ).filter(Objects::nonNull);
    }

    static Stream<Arguments> numberData() {
        var e1 = new PostgresJsonEntity(Map.of("a1", 1));
        var e2 = new PostgresJsonEntity(Map.of("a1", 2));
        var e3 = new PostgresJsonEntity(Map.of("a1", 3));
        var e4 = new PostgresJsonEntity(Map.of("a1", 3.14));
        var e5 = new PostgresJsonEntity(Map.of("a1", 42));
        var allCases = List.of(e1, e2, e3, e4, e5);
        return Stream.of(
                arguments(allCases, "properties.a1==1", List.of(e1)),
                arguments(allCases, "properties.a1==42", List.of(e5)),
                arguments(allCases, "properties.a1=ge=2", List.of(e2, e3, e4, e5)),
                arguments(allCases, "properties.a1=gt=2", List.of(e3, e4, e5)),
                arguments(allCases, "properties.a1=le=2", List.of(e1, e2)),
                arguments(allCases, "properties.a1=lt=2", List.of(e1)),
                arguments(allCases, "properties.a1=in=(1,2)", List.of(e1, e2)),
                arguments(allCases, "properties.a1=out=(1,2)", List.of(e3, e4, e5)),
                arguments(allCases, "properties.a1=bt=(3,4)", List.of(e3, e4)),
                arguments(allCases, "properties.a1=nb=(3,4)", List.of(e1, e2, e5)),
                null
        ).filter(Objects::nonNull);
    }

    protected static Stream<Arguments> dateData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "1970-01-01"));
        var e2 = new PostgresJsonEntity(Map.of("a", "2000-01-01"));
        var e3 = new PostgresJsonEntity(Map.of("a", "2020-01-01"));
        var e4 = new PostgresJsonEntity(Map.of("a", "2020-01-02"));
        var e5 = new PostgresJsonEntity(nullMap("a"));
        var e6 = new PostgresJsonEntity(Map.of("b", "other"));
        var e7 = new PostgresJsonEntity(Map.of("a", "not a date"));
        var allCases = List.of(e1, e2, e3, e4, e5, e6, e7);
        return Stream.of(
                arguments(allCases, "properties.a==1970-01-01", List.of(e1)),
                arguments(allCases, "properties.a=gt=1970-01-01", List.of(e2, e3, e4)),
                arguments(allCases, "properties.a>=1970-01-01", List.of(e1, e2, e3, e4)),
                arguments(allCases, "properties.a<1970-01-01", List.of()),
                arguments(allCases, "properties.a!=2020-01-01", List.of(e1, e2, e4, e5, e6, e7)),
                arguments(allCases, "properties.a=bt=(1970-01-01, 2020-01-01)", List.of(e1, e2, e3)),
                arguments(allCases, "properties.a=nb=(1970-01-01, 2020-01-01)", List.of(e4, e5, e6, e7)),
                null
        ).filter(Objects::nonNull);
    }

    protected static Stream<Arguments> timeData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "00:00:00"));
        var e2 = new PostgresJsonEntity(Map.of("a", "01:00:00"));
        var e3 = new PostgresJsonEntity(Map.of("a", "02:00:00"));
        var e4 = new PostgresJsonEntity(Map.of("a", "03:00:00"));
        var e5 = new PostgresJsonEntity(nullMap("a"));
        var e6 = new PostgresJsonEntity(Map.of("b", "other"));
        var e7 = new PostgresJsonEntity(Map.of("a", "not a time"));
        var allCases = List.of(e1, e2, e3, e4, e5, e6, e7);
        return Stream.of(
                arguments(allCases, "properties.a==00:00:00", List.of(e1)),
                arguments(allCases, "properties.a=gt=00:00:00", List.of(e2, e3, e4)),
                arguments(allCases, "properties.a>=00:00:00", List.of(e1, e2, e3, e4)),
                arguments(allCases, "properties.a<00:00:00", List.of()),
                arguments(allCases, "properties.a=bt=(00:00:00, 02:00:00)", List.of(e1, e2, e3)),
                arguments(allCases, "properties.a=nb=(00:00:00, 02:00:00)", List.of(e4, e5, e6, e7)),
                null
        ).filter(Objects::nonNull);
    }

    protected static Stream<Arguments> dateTimeWithTzData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "1970-01-01T00:00:00+00:00"));
        var e2 = new PostgresJsonEntity(Map.of("a", "2000-01-01T00:00:00+00:00"));
        var e3 = new PostgresJsonEntity(Map.of("a", "2020-01-01T00:00:00+00:00"));
        var e4 = new PostgresJsonEntity(Map.of("a", "2020-01-01T01:00:00+00:00"));
        var e5 = new PostgresJsonEntity(nullMap("a"));
        var e6 = new PostgresJsonEntity(Map.of("b", "other"));
        var allCases = List.of(e1, e2, e3, e4, e5, e6);
        return Stream.of(
                arguments(allCases, "properties.a==1970-01-01T00:00:00+00:00", List.of(e1)),
                arguments(allCases, "properties.a=gt=1970-01-01T00:00:00+00:00", List.of(e2, e3, e4)),
                arguments(allCases, "properties.a>=1970-01-01T00:00:00+00:00", List.of(e1, e2, e3, e4)),
                arguments(allCases, "properties.a<2020-01-01T00:00:00+00:00", List.of(e1, e2)),
                null
        ).filter(Objects::nonNull);
    }

    protected static Stream<Arguments> dateTimeWithoutTzData() {
        var e1 = new PostgresJsonEntity(Map.of("a", "1970-01-01T00:00:00"));
        var e2 = new PostgresJsonEntity(Map.of("a", "2000-01-01T00:00:00"));
        var e3 = new PostgresJsonEntity(Map.of("a", "2020-01-01T00:00:00"));
        var e4 = new PostgresJsonEntity(Map.of("a", "2020-01-01T01:00:00"));
        var e5 = new PostgresJsonEntity(nullMap("a"));
        var e6 = new PostgresJsonEntity(Map.of("b", "other"));
        var allCases = List.of(e1, e2, e3, e4, e5, e6);
        return Stream.of(
                arguments(allCases, "properties.a==1970-01-01T00:00:00", List.of(e1)),
                arguments(allCases, "properties.a=gt=1970-01-01T00:00:00", List.of(e2, e3, e4)),
                arguments(allCases, "properties.a>=1970-01-01T00:00:00", List.of(e1, e2, e3, e4)),
                arguments(allCases, "properties.a<2020-01-01T00:00:00", List.of(e1, e2)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> meltedTimeZone() {
        var e1 = new PostgresJsonEntity(Map.of("a", "1970-01-01T00:00:00"));
        var e2 = new PostgresJsonEntity(Map.of("a", "2000-01-01T00:00:00"));
        var e3 = new PostgresJsonEntity(Map.of("a", "2020-01-01T00:00:00"));
        var allCases = List.of(e1, e2, e3);
        return Stream.of(
            arguments(allCases, "properties.a=ge=1970-01-02T00:00:00+00:00", List.of(e2, e3)),
            arguments(allCases, "properties.a=ge=1970-01-02T00:00:00+01:00", List.of(e2, e3)),
            arguments(allCases, "properties.a=lt=2022-01-01T00:00:00+01:00", List.of(e1, e2, e3)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> arrayData() {
        var e1 = new PostgresJsonEntity(Map.of("a", List.of(1, 2, 3)));
        var e2 = new PostgresJsonEntity(Map.of("a", List.of(4, 5, 6)));
        var e3 = new PostgresJsonEntity(Map.of("a", List.of(7, 8, 9)));
        var allCases = List.of(e1, e2, e3);
        return Stream.of(
                arguments(allCases, "properties.a==1", List.of(e1)),
                arguments(allCases, "properties.a==4", List.of(e2)),
                arguments(allCases, "properties.a!=1", List.of(e2, e3)),
                arguments(allCases, "properties.a!=4", List.of(e1, e3)),
                arguments(allCases, "properties.a=in=(1,4)", List.of(e1, e2)),
                arguments(allCases, "properties.a=out=(1,4)", List.of(e3)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> nestedData() {
        var e1 = new PostgresJsonEntity(Map.of("a", Map.of("b", Map.of("c", 1))));
        var e2 = new PostgresJsonEntity(Map.of("a", Map.of("b", Map.of("c", 2))));
        var e3 = new PostgresJsonEntity(Map.of("a", Map.of("b", Map.of("c", 3))));
        var allCases = List.of(e1, e2, e3);
        return Stream.of(
                arguments(allCases, "properties.a.b.c==1", List.of(e1)),
                arguments(allCases, "properties.a.b.c==2", List.of(e2)),
                arguments(allCases, "properties.a.b.c!=1", List.of(e2, e3)),
                arguments(allCases, "properties.a.b.c!=2", List.of(e1, e3)),
                arguments(allCases, "properties.a.b.c=in=(1,2)", List.of(e1, e2)),
                arguments(allCases, "properties.a.b.c=out=(1,2)", List.of(e3)),
                arguments(allCases, "properties.a.b.c=bt=(1,2)", List.of(e1, e2)),
                arguments(allCases, "properties.a.b.c=nb=(1,2)", List.of(e3)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> doesNotConvertBoolean() {
        var e1 = new PostgresJsonEntity(Map.of("a", "true"));
        var e2 = new PostgresJsonEntity(Map.of("a", "false"));
        var e3 = new PostgresJsonEntity(Map.of("a", "other"));
        var allCases = List.of(e1, e2, e3);
        return Stream.of(
                arguments(allCases, "properties.a=like=true", List.of(e1)),
                arguments(allCases, "properties.a=like=false", List.of(e2)),
                arguments(allCases, "properties.a=in=(true,false)", List.of()),
                arguments(allCases, "properties.a=notlike=true", List.of(e2, e3)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> doesNotConvertInteger() {
        var e1 = new PostgresJsonEntity(Map.of("a", "1"));
        var e2 = new PostgresJsonEntity(Map.of("a", "2"));
        var e3 = new PostgresJsonEntity(Map.of("a", "other"));
        var allCases = List.of(e1, e2, e3);
        return Stream.of(
                arguments(allCases, "properties.a=like=1", List.of(e1)),
                arguments(allCases, "properties.a=like=2", List.of(e2)),
                arguments(allCases, "properties.a=in=(1,2)", List.of()),
                arguments(allCases, "properties.a=notlike=1", List.of(e2, e3)),
                arguments(allCases, "properties.a=ilike=2", List.of(e2)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> sortByText() {
        var e1 = new PostgresJsonEntity(Map.of("a", "abc", "b", 1));
        var e2 = new PostgresJsonEntity(Map.of("a", "ABC", "b", 2));
        var e3 = new PostgresJsonEntity(Map.of("a", "DEF", "b", 3));
        var e4 = new PostgresJsonEntity(Map.of("a", "GHI", "b", 4));
        var e5 = new PostgresJsonEntity(Map.of("a", "ghi", "b", 5));
        var e6 = new PostgresJsonEntity(Map.of("a", "klm", "b", 6));

        final var allCases = List.of(e1, e2, e3, e4, e5, e6);
        return Stream.of(
                arguments(allCases, "properties.a,asc", List.of(e1, e2, e3, e5, e4, e6)),
                arguments(allCases, "properties.a,desc", List.of(e6, e4, e5, e3, e2, e1)),
                arguments(allCases, "properties.a,asc,ic;properties.b", List.of(e1, e2, e3, e4, e5, e6)),
                arguments(allCases, "properties.a,desc,ic;properties.b,desc", List.of(e6, e5, e4, e3, e2, e1)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> sortByNumber() {
        var e1 = new PostgresJsonEntity(Map.of("b", 22, "a", 1, "z", 7));
        var e2 = new PostgresJsonEntity(Map.of("c", 10, "a", 2, "z", 3));
        var e3 = new PostgresJsonEntity(Map.of("d", 11, "a", 3, "z", 5));
        var e4 = new PostgresJsonEntity(Map.of("e", 3, "a", 11, "z", 1));
        var e5 = new PostgresJsonEntity(Map.of("f", 2, "a", 10, "z", 8));
        var e6 = new PostgresJsonEntity(Map.of("g", 1, "a", 22, "z", 0));

        var allCases = List.of(e1, e2, e3, e4, e5, e6);
        return Stream.of(
                arguments(allCases, "properties.a,asc", List.of(e1, e2, e3, e5, e4, e6)),
                arguments(allCases, "properties.a,desc", List.of(e6, e4, e5, e3, e2, e1)),
                arguments(allCases, "properties.z,asc", List.of(e6, e4, e2, e3, e1, e5)),
                arguments(allCases, "properties.z,desc", List.of(e5, e1, e3, e2, e4, e6)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> sortByNested() {
        var e1 = new PostgresJsonEntity(Map.of("0", "2", "a", Map.of("b", Map.of("c", 1))));
        var e2 = new PostgresJsonEntity(Map.of("1", "1", "a", Map.of("b", Map.of("c", 2))));
        var e3 = new PostgresJsonEntity(Map.of("2", "0", "a", Map.of("b", Map.of("c", 3))));
        var allCases = List.of(e1, e2, e3);
        return Stream.of(
                arguments(allCases, "properties.a.b.c,asc", List.of(e1, e2, e3)),
                arguments(allCases, "properties.a.b.c,desc", List.of(e3, e2, e1)),
                null
        ).filter(Objects::nonNull);
    }

    private static Stream<Arguments> sortByMixedData() {
        Map<String, Object> data6 = new HashMap<>();
        data6.put("a", null);
        data6.put("b", "def");
        var e1 = new PostgresJsonEntity(Map.of("a", "abc", "b", 987));
        var e2 = new PostgresJsonEntity(Map.of("a", "ABC", "b", "123"));
        var e3 = new PostgresJsonEntity(Map.of("a", 123, "b", false));
        var e4 = new PostgresJsonEntity(Map.of("a", true, "b", true));
        var e5 = new PostgresJsonEntity(Map.of("a", false, "b", "zyx"));
        var e6 = new PostgresJsonEntity(data6);

        var allCases = List.of(e1, e2, e3, e4, e5, e6);
        return Stream.of(
                arguments(allCases, "properties.a,asc", List.of(e6, e1, e2, e3, e5, e4)),
                arguments(allCases, "properties.a,desc", List.of(e4, e5, e3, e2, e1, e6)),
                arguments(allCases, "properties.a,asc,ic;properties.b", List.of(e3, e2, e1, e5, e6, e4)),
                arguments(allCases, "properties.a,desc,ic;properties.b,desc", List.of(e4, e6, e5, e1, e2, e3)),
                null
        ).filter(Objects::nonNull);
    }

    static Stream<Arguments> sortByRelation() {
        var e1 = JsonbEntity.builder().id(UUID.randomUUID()).data("{\"a\": 1, \"b\": 3}").build();
        var e2 = JsonbEntity.builder().id(UUID.randomUUID()).data("{\"a\": 2, \"b\": 2}").build();
        var e3 = JsonbEntity.builder().id(UUID.randomUUID()).data("{\"a\": 3, \"b\": 1}").build();
        var allCases = List.of(e1, e2, e3);
        return Stream.of(
                arguments(allCases, "jsonb.data.a,asc", List.of(e1, e2, e3)),
                arguments(allCases, "jsonb.data.b,asc", List.of(e3, e2, e1)),
                arguments(allCases, "jsonb.data,asc", List.of(e1, e2, e3)),
                arguments(allCases, "jsonb.data.a,desc", List.of(e3, e2, e1)),
                arguments(allCases, "jsonb.data.b,desc", List.of(e1, e2, e3)),
                arguments(allCases, "jsonb.data,desc", List.of(e3, e2, e1)),
                null
        ).filter(Objects::nonNull);
    }

    static Stream<Arguments> sortByMappedRelation() {
        var e1 = JsonbEntity.builder().id(UUID.randomUUID()).data("{\"a\": 1, \"b\": 3}").build();
        var e2 = JsonbEntity.builder().id(UUID.randomUUID()).data("{\"a\": 2, \"b\": 2}").build();
        var e3 = JsonbEntity.builder().id(UUID.randomUUID()).data("{\"a\": 3, \"b\": 1}").build();
        var allCases = List.of(e1, e2, e3);
        return Stream.of(
                arguments(allCases, "jsonb.data.a,asc", List.of(e1, e2, e3)),
                arguments(allCases, "jsonbRelation.a,asc", List.of(e1, e2, e3)),
                arguments(allCases, "jsonbRelation.b,asc", List.of(e3, e2, e1)),
                arguments(allCases, "jsonbRelation,asc", List.of(e1, e2, e3)),
                arguments(allCases, "jsonbRelation.a,desc", List.of(e3, e2, e1)),
                arguments(allCases, "jsonbRelation.b,desc", List.of(e1, e2, e3)),
                arguments(allCases, "jsonbRelation,desc", List.of(e3, e2, e1)),
                arguments(allCases, "jsonbRelationOfA,asc", List.of(e1, e2, e3)),
                arguments(allCases, "jsonbRelationOfA,desc", List.of(e3, e2, e1)),
                null
        ).filter(Objects::nonNull);
    }

    private static Map<String, Object> nullMap(String key) {
        HashMap<String, Object> nullValue = new HashMap<>();
        nullValue.put(key, null);
        return nullValue;
    }
}
