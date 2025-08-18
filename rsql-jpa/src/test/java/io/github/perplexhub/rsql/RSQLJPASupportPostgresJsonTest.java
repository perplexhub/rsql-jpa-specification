package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.perplexhub.rsql.model.PostgresJsonEntity;
import io.github.perplexhub.rsql.repository.jpa.postgres.PostgresJsonEntityRepository;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@SpringBootTest
@ActiveProfiles("postgres")
@RunWith(Parameterized.class)
public class RSQLJPASupportPostgresJsonTest {
  @ClassRule
  public static final SpringClassRule scr = new SpringClassRule();

  @Rule
  public final SpringMethodRule smr = new SpringMethodRule();

  @Autowired
  private PostgresJsonEntityRepository repository;
  @Autowired 
  private EntityManager em;
  
  private List<PostgresJsonEntity> users;
  private String rsql;
  private List<PostgresJsonEntity> expected;
  
  public RSQLJPASupportPostgresJsonTest(List<PostgresJsonEntity> users, String rsql, List<PostgresJsonEntity> expected) {
    this.users = users;
    this.rsql = rsql;
    this.expected = expected;
  }

  @Before
  public void setup() {
    Map<EntityManager, Database> map = new HashMap<>();
    map.put(em, Database.POSTGRESQL);
    RSQLVisitorBase.setEntityManagerDatabase(map);
  }

  @After
  public void tearDown() {
    repository.deleteAll();
    repository.flush();
    RSQLVisitorBase.setEntityManagerDatabase(new HashMap<>());
  }

  @Test
  public void testJson() {
    //given
    repository.saveAll(users);
    repository.flush();

    //when
    List<PostgresJsonEntity> result = repository.findAll(toSpecification(rsql));

    //then
    assertThat(result)
        .hasSameSizeAs(expected)
        .containsExactlyInAnyOrderElementsOf(expected);

    users.forEach(e -> e.setId(null));
  }

  @Parameters
  public static Collection<Object[]> data() {
    List<Object[]> data = new ArrayList<>();
    data.addAll(equalsData());
    data.addAll(inData());
    data.addAll(betweenData());
    data.addAll(likeData());
    data.addAll(gtLtData());
    data.addAll(miscData());
    return data;
  }

  private static Collection<Object[]> equalsData() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("a1", "b1");
    PostgresJsonEntity e1 = new PostgresJsonEntity(map1);
    
    Map<String, Object> innerMap = new HashMap<>();
    Map<String, Object> innerInnerMap = new HashMap<>();
    innerInnerMap.put("a111", "b1");
    innerMap.put("a11", innerInnerMap);
    Map<String, Object> map2 = new HashMap<>();
    map2.put("a1", innerMap);
    PostgresJsonEntity e2 = new PostgresJsonEntity(map2);

    PostgresJsonEntity e3 = new PostgresJsonEntity(e1);
    PostgresJsonEntity e4 = new PostgresJsonEntity(e2);

    Map<String, Object> map5 = new HashMap<>();
    map5.put("a", "b1");
    PostgresJsonEntity e5 = new PostgresJsonEntity(map5);
    
    Map<String, Object> map6 = new HashMap<>();
    map6.put("a", "b2");
    PostgresJsonEntity e6 = new PostgresJsonEntity(map6);
    
    Map<String, Object> map7 = new HashMap<>();
    map7.put("a", "c1");
    PostgresJsonEntity e7 = new PostgresJsonEntity(map7);

    List<Object[]> data = new ArrayList<>();
    data.add(new Object[] {Arrays.asList(e1, e2), "properties.a1==b1", Arrays.asList(e1)});
    data.add(new Object[] {Arrays.asList(e1, e2), "properties.a1!=b1", Arrays.asList(e2)});
    data.add(new Object[] {Arrays.asList(e1, e2), "properties.a1=ic=B1", Arrays.asList(e1)});
    data.add(new Object[] {Arrays.asList(e1, e2), "properties.a1==b2", Arrays.asList()});
    data.add(new Object[] {Arrays.asList(e3, e4), "properties.a1.a11.a111==b1", Arrays.asList(e4)});
    data.add(new Object[] {Arrays.asList(e3, e4), "properties.a1.a11.a111==b2", Arrays.asList()});

    data.add(new Object[] {Arrays.asList(e5, e6, e7), "properties.a==b*", Arrays.asList(e5, e6)});
    data.add(new Object[] {Arrays.asList(e5, e6, e7), "properties.a==c*", Arrays.asList(e7)});
    data.add(new Object[] {Arrays.asList(e5, e6, e7), "properties.a==*1", Arrays.asList(e5, e7)});
    
    return data;
  }

  private static Collection<Object[]> inData() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("a", "b1");
    PostgresJsonEntity e1 = new PostgresJsonEntity(map1);
    
    Map<String, Object> map2 = new HashMap<>();
    map2.put("a", "b2");
    PostgresJsonEntity e2 = new PostgresJsonEntity(map2);
    
    Map<String, Object> map3 = new HashMap<>();
    map3.put("a", "c1");
    PostgresJsonEntity e3 = new PostgresJsonEntity(map3);
    
    Map<String, Object> map4 = new HashMap<>();
    map4.put("a", "d1");
    PostgresJsonEntity e4 = new PostgresJsonEntity(map4);

    List<Object[]> data = new ArrayList<>();
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a=in=(b1, c1)", Arrays.asList(e1, e3)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a=out=(b1, c1)", Arrays.asList(e2, e4)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a=in=(b1)", Arrays.asList(e1)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a=out=(b1)", Arrays.asList(e2, e3, e4)});
    
    return data;
  }

  private static Collection<Object[]> betweenData() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("a", "a");
    PostgresJsonEntity e1 = new PostgresJsonEntity(map1);
    
    Map<String, Object> map2 = new HashMap<>();
    map2.put("a", "b");
    PostgresJsonEntity e2 = new PostgresJsonEntity(map2);
    
    Map<String, Object> map3 = new HashMap<>();
    map3.put("a", "c");
    PostgresJsonEntity e3 = new PostgresJsonEntity(map3);
    
    Map<String, Object> map4 = new HashMap<>();
    map4.put("a", "d");
    PostgresJsonEntity e4 = new PostgresJsonEntity(map4);

    List<Object[]> data = new ArrayList<>();
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a=bt=(a, c)", Arrays.asList(e1, e2, e3)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a=nb=(b, d)", Arrays.asList(e1)});
    
    return data;
  }

  private static Collection<Object[]> likeData() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("a", "a b c");
    PostgresJsonEntity e1 = new PostgresJsonEntity(map1);
    
    Map<String, Object> map2 = new HashMap<>();
    map2.put("a", "b c d");
    PostgresJsonEntity e2 = new PostgresJsonEntity(map2);
    
    Map<String, Object> map3 = new HashMap<>();
    map3.put("a", "c d e");
    PostgresJsonEntity e3 = new PostgresJsonEntity(map3);

    List<Object[]> data = new ArrayList<>();
    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ke='a b'", Arrays.asList(e1)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ke='b c'", Arrays.asList(e1, e2)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ke='c d'", Arrays.asList(e2, e3)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ke='d e'", Arrays.asList(e3)});

    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ik='A B'", Arrays.asList(e1)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ik='B C'", Arrays.asList(e1, e2)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ik='C D'", Arrays.asList(e2, e3)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ik='D E'", Arrays.asList(e3)});

    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=nk='a b'", Arrays.asList(e2, e3)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3), "properties.a=ni='A B'", Arrays.asList(e2, e3)});
    
    return data;
  }

  private static Collection<Object[]> gtLtData() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("a", "a");
    PostgresJsonEntity e1 = new PostgresJsonEntity(map1);
    
    Map<String, Object> map2 = new HashMap<>();
    map2.put("a", "b");
    PostgresJsonEntity e2 = new PostgresJsonEntity(map2);
    
    Map<String, Object> map3 = new HashMap<>();
    map3.put("a", "c");
    PostgresJsonEntity e3 = new PostgresJsonEntity(map3);
    
    Map<String, Object> map4 = new HashMap<>();
    map4.put("a", "d");
    PostgresJsonEntity e4 = new PostgresJsonEntity(map4);

    List<Object[]> data = new ArrayList<>();
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a>=a", Arrays.asList(e1, e2, e3, e4)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a>a", Arrays.asList(e2, e3, e4)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a<a", Arrays.asList()});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a<=a", Arrays.asList(e1)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a<b", Arrays.asList(e1)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a<=d", Arrays.asList(e1, e2, e3, e4)});
    
    return data;
  }

  private static Collection<Object[]> miscData() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("a", "b1");
    PostgresJsonEntity e1 = new PostgresJsonEntity(map1);
    
    Map<String, Object> map2 = new HashMap<>();
    map2.put("a", "b2");
    PostgresJsonEntity e2 = new PostgresJsonEntity(map2);
    
    Map<String, Object> map3 = new HashMap<>();
    map3.put("b", "c1");
    PostgresJsonEntity e3 = new PostgresJsonEntity(map3);
    
    Map<String, Object> map4 = new HashMap<>();
    map4.put("b", "d1");
    PostgresJsonEntity e4 = new PostgresJsonEntity(map4);

    List<Object[]> data = new ArrayList<>();
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a=nn=''", Arrays.asList(e1, e2)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.a=na=''", Arrays.asList(e3, e4)});

    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.b=nn=''", Arrays.asList(e3, e4)});
    data.add(new Object[] {Arrays.asList(e1, e2, e3, e4), "properties.b=na=''", Arrays.asList(e1, e2)});
    
    return data;
  }
}