package io.github.perplexhub.rsql;

import io.github.perplexhub.rsql.jsonb.JsonbPathSupport;
import io.github.perplexhub.rsql.model.PostgresJsonEntity;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static io.github.perplexhub.rsql.RSQLJPASupportPostgresJsonTest.*;

@SpringBootTest
@ActiveProfiles("postgres13")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RSQLJPASupportPostgresJsonPathTest {

    @Autowired
    private PostgresJsonEntityRepository repository;

    @BeforeEach
    void setup(@Autowired EntityManager em) {
        RSQLVisitorBase.setEntityManagerDatabase(Map.of(em, Database.POSTGRESQL));
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        repository.flush();
        RSQLVisitorBase.setEntityManagerDatabase(Map.of());
    }

    @ParameterizedTest
    @MethodSource("data")
    void testJson(List<PostgresJsonEntity> entities, String rsql, List<PostgresJsonEntity> expected) {
        //given
        repository.saveAllAndFlush(entities);

        //when
        List<PostgresJsonEntity> result = repository.findAll(toSpecification(JsonbPathSupport.query(rsql)));

        //then
        assertThat(result)
                .hasSameSizeAs(expected)
                .containsExactlyInAnyOrderElementsOf(expected);

        entities.forEach(e -> e.setId(null));
    }

    static Stream<Arguments> data() {
        return Stream.of(equalsData(),
                inData(),
                betweenData(),
                likeData(),
                gtLtData(),
                miscData(),
                textData(),
                booleanData(),
                numberData(),
                dateData(),
                timeData(),
                dateTimeWithTzData(),
                dateTimeWithoutTzData(),
                arrayData(),
                doesNotConvertBoolean(),
                doesNotConvertInteger(),
                nestedObjets(),
                null
        ).filter(Objects::nonNull).flatMap(s -> s);
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

    private static Stream<Arguments> nestedObjets() {
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
                null
        ).filter(Objects::nonNull);
    }

    private static Map<String, Object> nullMap(String key) {
        HashMap<String, Object> nullValue = new HashMap<>();
        nullValue.put(key, null);
        return nullValue;
    }
}
