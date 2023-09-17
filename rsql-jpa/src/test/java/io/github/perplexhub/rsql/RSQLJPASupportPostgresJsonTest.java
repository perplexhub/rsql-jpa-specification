package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.perplexhub.rsql.model.PostgresJsonEntity;
import io.github.perplexhub.rsql.repository.jpa.postgres.PostgresJsonEntityRepository;
import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("postgres")
class RSQLJPASupportPostgresJsonTest {

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
  void testJson(List<PostgresJsonEntity> users, String rsql, List<PostgresJsonEntity> expected) {
    //given
    repository.saveAllAndFlush(users);

    //when
    List<PostgresJsonEntity> result = repository.findAll(toSpecification(rsql));

    //then
    assertThat(result)
        .hasSameSizeAs(expected)
        .containsExactlyInAnyOrderElementsOf(expected);

    users.forEach(e -> e.setId(null));
  }

  @ParameterizedTest
  @MethodSource("legacyData")
  void testJsonDateForLegacyPostgres(List<PostgresJsonEntity> users, String rsql, List<PostgresJsonEntity> expected) {
    //given
    PostgresJsonPathExpressionBuilder.dateTimeSupport = false;
    repository.saveAllAndFlush(users);

    //when
    List<PostgresJsonEntity> result = repository.findAll(toSpecification(rsql));

    //then
    assertThat(result)
            .hasSameSizeAs(expected)
            .containsExactlyInAnyOrderElementsOf(expected);

    users.forEach(e -> e.setId(null));
  }

  static Stream<Arguments> legacyData() {
    return Stream.of(
              dateData(),
              timeData(),
              dateTimeWithTzData()
        )
        .flatMap(s -> s);
  }

  static Stream<Arguments> data() {
    return Stream.of(
            equalsData(),
            inData(),
            betweenData(),
            likeData(),
            gtLtData(),
            miscData(),
            numericData(),
            dateData(),
            timeData(),
            dateTimeWithTzData(),
            dateTimeWithoutTzData(),
            booleanData(),
            doesNotConvertBoolean(),
            doesNotConvertInteger()
        )
        .flatMap(s -> s);
  }

  private static Stream<Arguments> equalsData() {
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

  private static Stream<Arguments> inData() {
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

  private static Stream<Arguments> betweenData() {
    var e1 = new PostgresJsonEntity(Map.of("a", "a"));
    var e2 = new PostgresJsonEntity(Map.of("a", "b"));
    var e3 = new PostgresJsonEntity(Map.of("a", "c"));
    var e4 = new PostgresJsonEntity(Map.of("a", "d"));

    return Stream.of(
        arguments(List.of(e1, e2, e3, e4), "properties.a=bt=(a, c)", List.of(e1, e2, e3)),
        arguments(List.of(e1, e2, e3, e4), "properties.a=nb=(b, d)", List.of(e1))
    );
  }

  private static Stream<Arguments> likeData() {
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

  private static Stream<Arguments> gtLtData() {
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

  private static Stream<Arguments> miscData() {
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

  private static Stream<Arguments> numericData() {
    var e1 = new PostgresJsonEntity(Map.of("a", 1));
    var e2 = new PostgresJsonEntity(Map.of("a", 2));
    var e3 = new PostgresJsonEntity(Map.of("a", 3.14));
    var e4 = new PostgresJsonEntity(Map.of("a", 42));
    var e5 = new PostgresJsonEntity(nullMap("a"));
    var e6 = new PostgresJsonEntity(Map.of("b", "other"));

    return Stream.of(
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a==1", List.of(e1)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a==3.14", List.of(e3)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a>1", List.of(e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a>3.13", List.of(e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a>=3", List.of(e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a<3", List.of(e1, e2))
    );
  }


  protected static Stream<Arguments> dateData() {
    var e1 = new PostgresJsonEntity(Map.of("a", "1970-01-01"));
    var e2 = new PostgresJsonEntity(Map.of("a", "2000-01-01"));
    var e3 = new PostgresJsonEntity(Map.of("a", "2020-01-01"));
    var e4 = new PostgresJsonEntity(Map.of("a", "2020-01-02"));
    var e5 = new PostgresJsonEntity(nullMap("a"));
    var e6 = new PostgresJsonEntity(Map.of("b", "other"));

    return Stream.of(
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a==1970-01-01", List.of(e1)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a=gt=1970-01-01", List.of(e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a>=1970-01-01", List.of(e1, e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a<1970-01-01", List.of()),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a=bt=(1970-01-01, 2020-01-01)", List.of(e1, e2, e3)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a=nb=(1970-01-01, 2020-01-01)", List.of(e4))
    );
  }

  protected static Stream<Arguments> timeData() {
    var e1 = new PostgresJsonEntity(Map.of("a", "00:00:00"));
    var e2 = new PostgresJsonEntity(Map.of("a", "01:00:00"));
    var e3 = new PostgresJsonEntity(Map.of("a", "02:00:00"));
    var e4 = new PostgresJsonEntity(Map.of("a", "03:00:00"));
    var e5 = new PostgresJsonEntity(nullMap("a"));
    var e6 = new PostgresJsonEntity(Map.of("b", "other"));

    return Stream.of(
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a==00:00:00", List.of(e1)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a=gt=00:00:00", List.of(e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a>=00:00:00", List.of(e1, e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a<00:00:00", List.of()),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a=bt=(00:00:00, 02:00:00)", List.of(e1, e2, e3)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a=nb=(00:00:00, 02:00:00)", List.of(e4))
    );
  }

  protected static Stream<Arguments> dateTimeWithTzData() {
    var e1 = new PostgresJsonEntity(Map.of("a", "1970-01-01T00:00:00+00:00"));
    var e2 = new PostgresJsonEntity(Map.of("a", "2000-01-01T00:00:00+00:00"));
    var e3 = new PostgresJsonEntity(Map.of("a", "2020-01-01T00:00:00+00:00"));
    var e4 = new PostgresJsonEntity(Map.of("a", "2020-01-01T01:00:00+00:00"));
    var e5 = new PostgresJsonEntity(nullMap("a"));
    var e6 = new PostgresJsonEntity(Map.of("b", "other"));

    return Stream.of(
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a==1970-01-01T00:00:00+00:00", List.of(e1)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a=gt=1970-01-01T00:00:00+00:00", List.of(e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a>=1970-01-01T00:00:00+00:00", List.of(e1, e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a<2020-01-01T00:00:00+00:00", List.of(e1, e2))
    );
  }

  protected static Stream<Arguments> dateTimeWithoutTzData() {
    var e1 = new PostgresJsonEntity(Map.of("a", "1970-01-01T00:00:00"));
    var e2 = new PostgresJsonEntity(Map.of("a", "2000-01-01T00:00:00"));
    var e3 = new PostgresJsonEntity(Map.of("a", "2020-01-01T00:00:00"));
    var e4 = new PostgresJsonEntity(Map.of("a", "2020-01-01T01:00:00"));
    var e5 = new PostgresJsonEntity(nullMap("a"));
    var e6 = new PostgresJsonEntity(Map.of("b", "other"));

    return Stream.of(
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a==1970-01-01T00:00:00", List.of(e1)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a=gt=1970-01-01T00:00:00", List.of(e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a>=1970-01-01T00:00:00", List.of(e1, e2, e3, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a<2020-01-01T00:00:00", List.of(e1, e2))
    );
  }
  private static Stream<Arguments> booleanData() {
    var e1 = new PostgresJsonEntity(Map.of("a", true));
    var e2 = new PostgresJsonEntity(Map.of("a", false));
    var e3 = new PostgresJsonEntity(Map.of("a", true));
    var e4 = new PostgresJsonEntity(Map.of("a", false));
    var e5 = new PostgresJsonEntity(nullMap("a"));
    var e6 = new PostgresJsonEntity(Map.of("b", "other"));

    return Stream.of(
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a==true", List.of(e1, e3)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a==false", List.of(e2, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a!=true", List.of(e2, e4)),
            arguments(List.of(e1, e2, e3, e4, e5, e6), "properties.a!=false", List.of(e1, e3))
    );
  }

  private static Stream<Arguments> doesNotConvertBoolean() {
    var e1 = new PostgresJsonEntity(Map.of("a", "true"));
    var e2 = new PostgresJsonEntity(Map.of("a", "false"));
    var e3 = new PostgresJsonEntity(Map.of("a", "other"));

    return Stream.of(
            arguments(List.of(e1, e2, e3), "properties.a=like=true", List.of(e1)),
            arguments(List.of(e1, e2, e3), "properties.a=like=false", List.of(e2)),
            arguments(List.of(e1, e2, e3), "properties.a=in=(true,false)", List.of()),
            arguments(List.of(e1, e2, e3), "properties.a=notlike=true", List.of(e2, e3))
    );
  }

  private static Stream<Arguments> doesNotConvertInteger() {
    var e1 = new PostgresJsonEntity(Map.of("a", "1"));
    var e2 = new PostgresJsonEntity(Map.of("a", "2"));
    var e3 = new PostgresJsonEntity(Map.of("a", "other"));

    return Stream.of(
            arguments(List.of(e1, e2, e3), "properties.a=like=1", List.of(e1)),
            arguments(List.of(e1, e2, e3), "properties.a=like=2", List.of(e2)),
            arguments(List.of(e1, e2, e3), "properties.a=in=(1,2)", List.of()),
            arguments(List.of(e1, e2, e3), "properties.a=notlike=1", List.of(e2, e3))
    );
  }

  private static Map<String, Object> nullMap(String key) {
    HashMap<String, Object> nullValue = new HashMap<>();
    nullValue.put(key, null);
    return nullValue;
  }
}
