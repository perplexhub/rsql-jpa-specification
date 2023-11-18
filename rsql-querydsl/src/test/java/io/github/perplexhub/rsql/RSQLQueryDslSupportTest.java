package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLJPASupport.*;
import static io.github.perplexhub.rsql.RSQLQueryDslSupport.*;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.querydsl.core.types.dsl.BooleanExpression;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.perplexhub.rsql.model.*;
import io.github.perplexhub.rsql.repository.querydsl.CompanyRepository;
import io.github.perplexhub.rsql.repository.querydsl.TrunkGroupRepository;
import io.github.perplexhub.rsql.repository.querydsl.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
class RSQLQueryDslSupportTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TrunkGroupRepository trunkGroupRepository;

	@Test
	final void testQueryMultiLevelAttribute() {
		String rsql = "projects.projectTag.localTag.description=='Local Tag 1'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getId(), equalTo(1));
	}

		@Test
	final void testEnumILike() {
		RSQLJPASupport.addEntityAttributeTypeMap(Status.class, String.class);
		String rsql = "status==*A*";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));
	}

	@Test
	final void testElementCollection1() {
		String rsql = "tags=='tech'";
		List<Company> companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "tags=like='ec'";
		companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));
	}

	@Test
	final void testElementCollection2() {
		String rsql = "bigTags.tag=='tech'";
		List<Company> companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));
	}

	@Test
	final void testDoubleAssociation() {
		String rsql = "sites.trunks.id==2";
		List<TrunkGroup> trunkGroups = (List<TrunkGroup>) trunkGroupRepository.findAll(toPredicate(rsql, QTrunkGroup.trunkGroup));
		long count = trunkGroups.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("DoubleAssociation", count, is(0L));
	}

	@Test
	final void testNull() {
		String rsql = null;
		long count = userRepository.count(toPredicate(rsql, QUser.user));
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("Null", count, is(12L));

		count = companyRepository.count(toPredicate(rsql, QUser.user));
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("Null", count, is(9L));
	}

	@Test
	final void testEqual() {
		String rsql = "id==2";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "id=='2'";
		List<Company> companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getName(), equalTo("World Inc"));

		rsql = "company.id=='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));
		assertThat(rsql, users.get(0).getName(), equalTo("March"));
		assertThat(rsql, users.get(1).getName(), equalTo("April"));
		assertThat(rsql, users.get(2).getName(), equalTo("May"));
		assertThat(rsql, users.get(3).getName(), equalTo("June"));
		assertThat(rsql, users.get(0).getCompany().getName(), equalTo("World Inc"));

		rsql = "name==''";
		companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("empty"));
		assertThat(rsql, companys.get(0).getName(), equalTo(""));

		rsql = "userRoles.id.roleId=='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "userRoles.role.code=='admin'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testNotEqual() {
		String rsql = "id!='2'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11L));

		rsql = "company.id!='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));
	}

	@Test
	final void testGreaterThan() {
		String rsql = "id>'2'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));

		rsql = "company.id=gt='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testGreaterThanCreateDate() {
		String rsql = "createDate=gt=2018-10-20";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2L));
	}

	@Test
	final void testLessThanCreateDate() {
		String rsql = "createDate=lt=2018-10-20";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));
	}

	@Test
	final void testGreaterThanOrEqual() {
		String rsql = "id>='2'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11L));

		rsql = "company.id=ge='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));

		rsql = "userRoles.id.roleId>='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "userRoles.role.id>='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testLessThan() {
		String rsql = "id<'2'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));

		rsql = "company.id=lt='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2L));
	}

	@Test
	final void testLessThanOrEqual() {
		String rsql = "id<='2'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2L));

		rsql = "company.id=le='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testIn() {
		String rsql = "id=in=('2')";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));

		rsql = "company.id=in=('2')";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "company.id=in='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "company.id=in=(2,3)";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.id=in=('2','4')";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(5L));
	}

	@Test
	final void testNotIn() {
		String rsql = "id=out=('2')";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11L));

		rsql = "company.id=out=('2')";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));

		rsql = "company.id=out='2'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));

		rsql = "company.id=out=(2,3)";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.id=out=('2','4')";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7L));
	}

	@Test
	final void testIsNull() {
		String rsql = "name=isnull=''";
		List<Company> companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));

		rsql = "name=null=''";
		companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));

		rsql = "name=na=''";
		companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));
	}

	@Test
	final void testIsNotNull() {
		String rsql = "name=isnotnull=''";
		List<Company> companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));

		rsql = "name=notnull=''";
		companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));

		rsql = "name=nn=''";
		companys = (List<Company>) companyRepository.findAll(toPredicate(rsql, QCompany.company));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));
	}

	@Test
	final void testLike() {
		String rsql = "name=like='ber'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "company.name=like='Inc'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='*Inc*'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='*Inc'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='Inc*'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(0L));
	}

	@Test
	final void testNotLike() {
		String rsql = "name=notlike='ber'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));

		rsql = "company.name=notlike='Inc'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testEqualsIgnoreCase() {
		String rsql = "name=icase='may'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));

		rsql = "company.name=icase='fake company'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "company.name=='^fake company'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testLikeIgnoreCase() {
		String rsql = "name=ilike='BER'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "company.name=ilike='INC'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='^*INC*'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='^*INC'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='^INC*'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(0L));
	}

	@Test
	final void testNotLikeIgnoreCase() {
		String rsql = "name=inotlike='BER'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));

		rsql = "company.name=inotlike='INC'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testAnd() {
		String rsql = "company.id=in=(2,5);userRoles.role.code=='admin'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "company.id=in=(2,5) and userRoles.role.code=='admin'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testOr() {
		String rsql = "company.id=in=(2,5),userRoles.role.code=='admin'";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7L));

		rsql = "company.id=in=(2,5) or userRoles.role.code=='admin'";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7L));
	}

	@Test
	final void testBetween() {
		String rsql = "id=bt=('2', '4')";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "company.id=bt=(2,'5')";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));
	}

	@Test
	final void testNotBetween() {
		String rsql = "id=nb=('2', '4')";
		List<User> users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(9L));

		rsql = "company.id=nb=(2,'5')";
		users = (List<User>) userRepository.findAll(toPredicate(rsql, QUser.user));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2L));
	}

	@Test
	final void testPropertyPathMapper() {
		Map<String, String> propertyPathMapper = new HashMap<>();
		propertyPathMapper.put("i", "id");
		propertyPathMapper.put("ci", "company.id");
		propertyPathMapper.put("n", "name");
		propertyPathMapper.put("urir", "userRoles.id.roleId");
		propertyPathMapper.put("urrc", "userRoles.role.code");
		String rsql = "i==2";
		final BooleanExpression booleanExpression1 = toPredicate(rsql, QUser.user, propertyPathMapper);
		assert booleanExpression1 != null;
		List<User> users = (List<User>) userRepository.findAll(booleanExpression1);
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "i=='2'";
		final BooleanExpression booleanExpression2 = toPredicate(rsql, QCompany.company, propertyPathMapper);
		assert booleanExpression2 != null;
		List<Company> companys = (List<Company>) companyRepository.findAll(booleanExpression2);
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getName(), equalTo("World Inc"));

		rsql = "ci=='2'";
		final BooleanExpression booleanExpression3 = toPredicate(rsql, QUser.user, propertyPathMapper);
		assert booleanExpression3 != null;
		users = (List<User>) userRepository.findAll(booleanExpression3);
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));
		assertThat(rsql, users.get(0).getName(), equalTo("March"));
		assertThat(rsql, users.get(1).getName(), equalTo("April"));
		assertThat(rsql, users.get(2).getName(), equalTo("May"));
		assertThat(rsql, users.get(3).getName(), equalTo("June"));
		assertThat(rsql, users.get(0).getCompany().getName(), equalTo("World Inc"));

		rsql = "n==''";
		final BooleanExpression booleanExpression4 = toPredicate(rsql, QCompany.company, propertyPathMapper);
		assert booleanExpression4 != null;
		companys = (List<Company>) companyRepository.findAll(booleanExpression4);
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("empty"));
		assertThat(rsql, companys.get(0).getName(), equalTo(""));

		rsql = "urir=='2'";
		final BooleanExpression booleanExpression5 = toPredicate(rsql, QUser.user, propertyPathMapper);
		assert booleanExpression5 != null;
		users = (List<User>) userRepository.findAll(booleanExpression5);
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "urrc=='admin'";
		final BooleanExpression booleanExpression6 = toPredicate(rsql, QUser.user, propertyPathMapper);
		assert booleanExpression6 != null;
		users = (List<User>) userRepository.findAll(booleanExpression6);
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	void testThrowUnknownPropertyException() {
		assertThatExceptionOfType(UnknownPropertyException.class)
				.isThrownBy(() -> toPredicate("abc==1", QUser.user))
				.satisfies(e -> {
					assertEquals("abc", e.getName());
					assertEquals(User.class, e.getType());
				});
	}

}
