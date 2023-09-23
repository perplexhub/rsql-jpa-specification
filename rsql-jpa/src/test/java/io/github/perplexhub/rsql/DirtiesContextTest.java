package io.github.perplexhub.rsql;

import io.github.perplexhub.rsql.model.PostgresJsonEntity;
import io.github.perplexhub.rsql.repository.jpa.postgres.PostgresJsonEntityRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("postgres")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DirtiesContextTest {

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
	@Test
	final void firstTest() {
		PostgresJsonEntity entity = new PostgresJsonEntity();
		entity.setProperties(Map.of("id", 2));
		repository.saveAndFlush(entity);
		List<PostgresJsonEntity> entities = repository.findAll(toSpecification("properties.id==2"));
		assertThat(entities.size(), is(1));

	}
	@Test
	final void secondTest() {
		PostgresJsonEntity entity = new PostgresJsonEntity();
		entity.setProperties(Map.of("id", 2));
		repository.saveAndFlush(entity);
		List<PostgresJsonEntity> entities = repository.findAll(toSpecification("properties.id==2"));
		assertThat(entities.size(), is(1));
	}


}
