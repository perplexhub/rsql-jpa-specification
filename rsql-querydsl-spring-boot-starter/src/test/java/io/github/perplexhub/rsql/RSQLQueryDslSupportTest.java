package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLJPASupport.*;
import static io.github.perplexhub.rsql.RSQLQueryDslSupport.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import io.github.perplexhub.rsql.model.Company;
import io.github.perplexhub.rsql.model.QCompany;
import io.github.perplexhub.rsql.model.QUser;
import io.github.perplexhub.rsql.model.User;
import io.github.perplexhub.rsql.repository.querydsl.CompanyRepository;
import io.github.perplexhub.rsql.repository.querydsl.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.NONE)
public class RSQLQueryDslSupportTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Test
	public final void testEqual() {
		String rsql = "id==2";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "id=='2'";
		List<Company> companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getName(), equalTo("World Inc"));

		rsql = "company.id=='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));
		assertThat(rsql, users.get(0).getName(), equalTo("March"));
		assertThat(rsql, users.get(1).getName(), equalTo("April"));
		assertThat(rsql, users.get(2).getName(), equalTo("May"));
		assertThat(rsql, users.get(3).getName(), equalTo("June"));
		assertThat(rsql, users.get(0).getCompany().getName(), equalTo("World Inc"));

		rsql = "name==''";
		companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getCode(), equalTo("empty"));
		assertThat(rsql, companys.get(0).getName(), equalTo(""));

		rsql = "userRoles.id.roleId=='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		rsql = "userRoles.role.code=='admin'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));
	}

	@Test
	public final void testBetweenDateTime() {
		String rsql = "createDate=bt=('2018-01-01 12:34:56', '2018-12-31 10:34:56')";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11l));
	}

}
