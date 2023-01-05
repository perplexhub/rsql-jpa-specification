package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLJPASupport.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.perplexhub.rsql.model.Company;
import io.github.perplexhub.rsql.model.User;
import io.github.perplexhub.rsql.repository.jpa.CompanyRepository;
import io.github.perplexhub.rsql.repository.jpa.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
class RSQLSpecificationSupportTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Test
	final void testEqual() {
		String rsql = "id==2";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "id=='2'";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getName(), equalTo("World Inc"));

		rsql = "company.id=='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));
		assertThat(rsql, users.get(0).getName(), equalTo("March"));
		assertThat(rsql, users.get(1).getName(), equalTo("April"));
		assertThat(rsql, users.get(2).getName(), equalTo("May"));
		assertThat(rsql, users.get(3).getName(), equalTo("June"));
		assertThat(rsql, users.get(0).getCompany().getName(), equalTo("World Inc"));

		rsql = "name==''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("empty"));
		assertThat(rsql, companys.get(0).getName(), equalTo(""));

		rsql = "userRoles.id.roleId=='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "userRoles.role.code=='admin'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

}
