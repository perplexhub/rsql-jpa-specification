package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLCommonSupport.*;
import static io.github.perplexhub.rsql.RSQLJPASupport.*;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import io.github.perplexhub.rsql.model.Status;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.perplexhub.rsql.model.Company;
import io.github.perplexhub.rsql.model.Role;
import io.github.perplexhub.rsql.model.TrunkGroup;
import io.github.perplexhub.rsql.model.User;
import io.github.perplexhub.rsql.repository.jpa.CompanyRepository;
import io.github.perplexhub.rsql.repository.jpa.TrunkGroupRepository;
import io.github.perplexhub.rsql.repository.jpa.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
class RSQLJPASupportTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TrunkGroupRepository trunkGroupRepository;

	@Test
	final void testQuerySupport() {
		QuerySupport querySupport = QuerySupport.builder().rsqlQuery("id==2").build();
		List<User> users = userRepository.findAll(toSpecification(querySupport));
		long count = users.size();
		log.info("rsql: {} -> count: {}", querySupport.getRsqlQuery(), count);
		assertThat(querySupport.getRsqlQuery(), count, is(1L));
		assertThat(querySupport.getRsqlQuery(), users.get(0).getName(), equalTo("February"));
	}

	@Test
	final void testQuerySupportShorthand() {
		QuerySupport querySupport = Q.rsql("id==2").build();
		List<User> users = userRepository.findAll(toSpecification(querySupport));
		long count = users.size();
		log.info("rsql: {} -> count: {}", querySupport.getRsqlQuery(), count);
		assertThat(querySupport.getRsqlQuery(), count, is(1L));
		assertThat(querySupport.getRsqlQuery(), users.get(0).getName(), equalTo("February"));
	}

	@Test
	final void testJoinHints() {
		// use left join by default for one to many
		String rsql = "projects.departmentName==someDepartmentName";
		List<User> users = userRepository.findAll(toSpecification(rsql, true));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));

		// change to inner join
		Map<String, JoinType> joinHints = Map.of("User.projects", JoinType.INNER);
		users = userRepository.findAll(toSpecification(rsql, true,null, joinHints));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
	}

	@Test
	final void testJoinHintsRelationIsNullOrRelationPropertyIsSomeValues() {
		String rsql = "city.name=='Hong Kong Island',city.parent=na=''";
		Map<String, JoinType> joinHints = Map.of(
				"User.city", JoinType.INNER,
				"City.parent", JoinType.LEFT
		);
		List<User> users = userRepository.findAll(toSpecification(rsql, true,null, joinHints));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testCustomPredicateIsNull() {
		String rsql = "city=notAssigned=''";
		RSQLCustomPredicate<String> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=notAssigned="), String.class,
				input -> input.getCriteriaBuilder().isNull(input.getRoot().get("city")));
		List<User> users = userRepository.findAll(toSpecification(rsql, List.of(customPredicate)));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(9L));
	}

	@Test
	final void testCustomPredicateBetween() {
		String rsql = "company.id=between=(2,3)";
		RSQLCustomPredicate<Long> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=between=", true), Long.class,
				input -> input.getCriteriaBuilder().between(input.getPath().as(Long.class), (Long) input.getArguments().get(0), (Long) input.getArguments().get(1)));
		List<User> users = userRepository.findAll(toSpecification(rsql, List.of(customPredicate)));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testCustomPredicate() {
		String rsql = "createDate=dayofweek='2'";
		RSQLCustomPredicate<Long> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=dayofweek="), Long.class, input -> {
			Expression<Long> function = input.getCriteriaBuilder().function("DAY_OF_WEEK", Long.class, input.getPath());
			return input.getCriteriaBuilder().lessThan(function, (Long) input.getArguments().get(0));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, List.of(customPredicate)));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2L));

		rsql = "createDate=dayofweek='3'";
		users = userRepository.findAll(toSpecification(rsql, List.of(customPredicate)));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L)); //While this is in the old repo, I think something is off
	}

	@Test
	final void testCustomPredicateSimilar() {
		String rsql = "name=around='May'";
		RSQLCustomPredicate<String> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=around="), String.class, input -> {
			if ("May".equals(input.getArguments().get(0))) {
				return input.getPath().in(List.of("April", "May", "June"));
			}
			return input.getCriteriaBuilder().equal(input.getPath(), input.getArguments().get(0));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, List.of(customPredicate)));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "name=around='June'";
		users = userRepository.findAll(toSpecification(rsql, List.of(customPredicate)));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
	}

	@Test
	final void testElementCollection1() {
		String rsql = "tags=='tech'";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));
	}

	@Test
	final void testElementCollection2() {
		String rsql = "bigTags.tag=='tech'";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));
	}

	@Test
	final void testToComplexMultiValueMap() {
		String rsql = "sites.trunks.id==2,id=na=2,company.id=='2',id=na=3,name==''";
		Map<String, MultiValueMap<String, String>> map = toComplexMultiValueMap(rsql);
		log.info("Map<String, MultiValueMap<String, String>> map:{}", map);
		long count = map.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("ComplexMultiValueMap", count, is(4L));
		assertThat("ComplexMultiValueMap", map.get("company.id").get("==").size(), is(1));
		assertThat("ComplexMultiValueMap", map.get("id").get("=null=").size(), is(2));
		assertThat("ComplexMultiValueMap", map.get("id").get("=isnull=").size(), is(2));
		assertThat("ComplexMultiValueMap", map.get("id").get("=na=").size(), is(2));
	}

	@Test
	final void testToMultiValueMap() {
		String rsql = "sites.trunks.id==2,id==2,company.id=='2',id==3,name==''";
		MultiValueMap<String, String> map = toMultiValueMap(rsql);
		log.info("MultiValueMap<String,String> map:{}", map);
		long count = map.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("MultiValueMap", count, is(4L));
		assertThat("MultiValueMap", map.get("id").size(), is(2));
	}

	@Test
	final void testDoubleAssociation() {
		String rsql = "sites.trunks.id==2";
		List<TrunkGroup> trunkGroups = trunkGroupRepository.findAll(toSpecification(rsql));
		long count = trunkGroups.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("DoubleAssociation", count, is(0L));
	}

	@Test
	final void testNull() {
		String rsql = null;
		long count = userRepository.count(toSpecification(rsql));
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("Null", count, is(12L));

		count = companyRepository.count(toSpecification(rsql));
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("Null", count, is(7L));
	}

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

	@Test
	final void testNotEqual() {
		String rsql = "id!='2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11L));

		rsql = "company.id!='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));
	}

	@Test
	final void testGreaterThan() {
		String rsql = "id>'2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));

		rsql = "company.id=gt='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testGreaterThanOrEqual() {
		String rsql = "id>='2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11L));

		rsql = "company.id=ge='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));

		rsql = "userRoles.id.roleId>='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "userRoles.role.id>='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testLessThan() {
		String rsql = "id<'2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));

		rsql = "company.id=lt='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2L));
	}

	@Test
	final void testLessThanOrEqual() {
		String rsql = "id<='2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2L));

		rsql = "company.id=le='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testIn() {
		String rsql = "id=in=('2')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));

		rsql = "company.id=in=('2')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "company.id=in='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "company.id=in=(2,3)";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.id=in=('2','4')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(5L));
	}

	@Test
	final void testNotIn() {
		String rsql = "id=out=('2')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11L));

		rsql = "company.id=out=('2')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));

		rsql = "company.id=out='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));

		rsql = "company.id=out=(2,3)";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.id=out=('2','4')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7L));
	}

	@Test
	final void testIsNull() {
		String rsql = "name=isnull=''";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));

		rsql = "name=null=''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));

		rsql = "name=na=''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));
	}

	@Test
	final void testIsNotNull() {
		String rsql = "name=isnotnull=''";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));

		rsql = "name=notnull=''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));

		rsql = "name=nn=''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));
	}

	@Test
	final void testLike() {
		String rsql = "name=like='ber'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "company.name=like='Inc'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='*Inc*'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='*Inc'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='Inc*'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(0L));
	}

	@Test
	final void testNotLike() {
		String rsql = "name=notlike='ber'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));

		rsql = "company.name=notlike='Inc'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testEqualsIgnoreCase() {
		String rsql = "name=icase='may'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));

		rsql = "company.name=icase='fake company'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "company.name=='^fake company'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testLikeIgnoreCase() {
		String rsql = "name=ilike='BER'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));

		rsql = "company.name=ilike='INC'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='^*INC*'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='^*INC'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));

		rsql = "company.name=='^INC*'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(0L));
	}

	@Test
	final void testNotLikeIgnoreCase() {
		String rsql = "name=inotlike='BER'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8L));

		rsql = "company.name=inotlike='INC'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6L));
	}

	@Test
	final void testAnd() {
		String rsql = "company.id=in=(2,5);userRoles.role.code=='admin'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "company.id=in=(2,5) and userRoles.role.code=='admin'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testOr() {
		String rsql = "company.id=in=(2,5),userRoles.role.code=='admin'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7L));

		rsql = "company.id=in=(2,5) or userRoles.role.code=='admin'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7L));
	}

	@Test
	final void testBetweenDate() {
		String rsql = "createDate=bt=('2018-01-01', '2018-10-31')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));
	}

	@Test
	final void testBetweenDateTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		RSQLJPASupport.addConverter(Date.class, s -> {
			try {
				return sdf.parse(s);
			} catch (ParseException e) {
				return null;
			}
		});
		String rsql = "createDate=bt=('2018-01-01 12:34:56', '2018-12-31 10:34:56')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11L));
		RSQLJPASupport.removeConverter(Date.class);
	}

	@Test
	final void testBetween() {
		String rsql = "id=bt=('2', '4')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "company.id=bt=(2,'5')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10L));
	}

	@Test
	final void testNotBetween() {
		String rsql = "id=nb=('2', '4')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(9L));

		rsql = "company.id=nb=(2,'5')";
		users = userRepository.findAll(toSpecification(rsql));
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
		List<User> users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "i=='2'";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getName(), equalTo("World Inc"));

		rsql = "ci=='2'";
		users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4L));
		assertThat(rsql, users.get(0).getName(), equalTo("March"));
		assertThat(rsql, users.get(1).getName(), equalTo("April"));
		assertThat(rsql, users.get(2).getName(), equalTo("May"));
		assertThat(rsql, users.get(3).getName(), equalTo("June"));
		assertThat(rsql, users.get(0).getCompany().getName(), equalTo("World Inc"));

		rsql = "n==''";
		companys = companyRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, companys.get(0).getCode(), equalTo("empty"));
		assertThat(rsql, companys.get(0).getName(), equalTo(""));

		rsql = "urir=='2'";
		users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));

		rsql = "urrc=='admin'";
		users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3L));
	}

	@Test
	final void testWhitelist() {
		addPropertyWhitelist(User.class, "id");
		addPropertyWhitelist(Role.class, "id");
		Map<String, String> propertyPathMapper = new HashMap<>();
		propertyPathMapper.put("i", "id");
		propertyPathMapper.put("urrc", "userRoles.role.code");
		String rsql = "i==2";
		List<User> users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		String rsql2 = "urrc=='admin'";
		final Specification<User> toSpecification = toSpecification(rsql2, propertyPathMapper);
		assertThatExceptionOfType(PropertyNotWhitelistedException.class)
				.isThrownBy(() -> userRepository.findAll(toSpecification))
				.satisfies(e -> {
					assertEquals("code", e.getName());
					assertEquals(e.getType(), Role.class);
				});
	}

	@Test
	final void testBlacklist() {
		addPropertyBlacklist(User.class, "name");
		addPropertyBlacklist(Role.class, "code");
		Map<String, String> propertyPathMapper = new HashMap<>();
		propertyPathMapper.put("i", "id");
		propertyPathMapper.put("urrc", "userRoles.role.code");
		String rsql = "i==2";
		List<User> users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		String rsql2 = "urrc=='admin'";
		final Specification<User> toSpecification = toSpecification(rsql2, propertyPathMapper);
		assertThatExceptionOfType(PropertyBlacklistedException.class)
				.isThrownBy(() -> userRepository.findAll(toSpecification))
				.satisfies(e -> {
					assertEquals("code", e.getName());
					assertEquals(e.getType(), Role.class);
				});
	}

	@Test
	final void testInlineWhitelist() {
		addPropertyWhitelist(User.class, "urrc");
		addPropertyWhitelist(Role.class, "code");
		Map<String, String> propertyPathMapper = new HashMap<>();
		propertyPathMapper.put("i", "id");
		propertyPathMapper.put("urrc", "userRoles.role.code");
		String rsql = "i==2";
		Map<Class<?>, List<String>> propertyWhitelist = new HashMap<>();
		propertyWhitelist.put(User.class, Collections.singletonList("id"));
		propertyWhitelist.put(Role.class, Collections.singletonList("id")); // overwrite global whitelist with our supplied whitelist
		List<User> users = userRepository.findAll(toSpecification(rsql, propertyPathMapper, null, null, propertyWhitelist, null));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		String rsql2 = "urrc=='admin'";
		final Specification<User> toSpecification = toSpecification(rsql2, propertyPathMapper,
				null, null, propertyWhitelist, null);
		assertThatExceptionOfType(PropertyNotWhitelistedException.class)
				.isThrownBy(() -> userRepository.findAll(toSpecification))
				.satisfies(e -> {
					assertEquals("code", e.getName());
					assertEquals(e.getType(), Role.class);
				});
	}

	@Test
	final void testInlineBlacklist() {
		addPropertyBlacklist(User.class, "name");
		addPropertyBlacklist(Role.class, "name");
		Map<String, String> propertyPathMapper = new HashMap<>();
		propertyPathMapper.put("i", "id");
		propertyPathMapper.put("urrc", "userRoles.role.code");
		String rsql = "i==2";
		Map<Class<?>, List<String>> propertyBlacklist = new HashMap<>();
		propertyBlacklist.put(Role.class, Collections.singletonList("code")); // overwrite global whitelist with our supplied whitelist
		List<User> users = userRepository.findAll(toSpecification(rsql, propertyPathMapper, null, null, null, propertyBlacklist));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1L));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		String rsql2 = "urrc=='admin'";
		final Specification<User> toSpecification = toSpecification(rsql2, propertyPathMapper,
				null, null, null, propertyBlacklist);
		assertThatExceptionOfType(PropertyBlacklistedException.class)
				.isThrownBy(() -> userRepository.findAll(toSpecification))
				.satisfies(e -> {
					assertEquals("code", e.getName());
					assertEquals(e.getType(), Role.class);
				});
	}

	@Test
	void testWithNullSort() {
		Page<User> users = userRepository.findAll(toSort((String)null), PageRequest.of(0, 5));
		Assertions.assertThat(users.getContent())
			.extracting(User::getId)
			.containsExactly(1, 2, 3, 4, 5);
	}
	
	@Test
	void testWithMalformedSort() {
		Page<User> users = userRepository.findAll(toSort(";;,;,,;"), PageRequest.of(0, 1));

		Assertions.assertThat(users.getContent())
			.extracting(User::getId)
			.containsExactly(1);
	}

	@Test
	void testSortUsersByIdDesc() {
		Page<User> users = userRepository.findAll(toSort("id,desc"), PageRequest.of(0, 5));
		Assertions.assertThat(users.getContent())
			.extracting(User::getId)
			.containsExactly(12, 11, 10, 9, 8);
	}

	@Test
	void testSortMultipleFields() {
		List<User> users = userRepository.findAll(toSort("status,desc;company.id,desc"));
		Assertions.assertThat(users)
			.extracting(User::getId)
			.containsExactly(3, 4, 5, 1, 2, 10, 11, 12, 9, 7, 8, 6);
	}

	@Test
	void testSortAndFilter() {
		Specification<User> specification = RSQLJPASupport.<User>toSpecification("status==ACTIVE")
			.and(toSort("company.name,desc"));

		List<User> users = userRepository.findAll(specification);
		Assertions.assertThat(users)
			.extracting(User::getId)
			.containsExactly(6, 9, 7, 8);
	}
	
	@Test
	void testSortDefaultAsc() {
		Specification<User> specification = toSort("name");

		List<User> users = userRepository.findAll(specification);

		Assertions.assertThat(users)
			.extracting(User::getName)
			.isSortedAccordingTo(String::compareTo); // asc
	}

	@Test
	void testSortWithCustomPropertyMapping() {
		Map<String, String> propertyMapping = Map.of(
				"userStatus", "status",
				"companyId", "company.id"
		);
		List<User> users = userRepository.findAll(toSort("userStatus,desc;companyId,desc", propertyMapping));
		Assertions.assertThat(users)
			.extracting(User::getId)
			.containsExactly(3, 4, 5, 1, 2, 10, 11, 12, 9, 7, 8, 6);
	}

	@Test
	void testSortIgnoreCase() {
		List<Company> companies = companyRepository.findAll(toSort("code,desc,ic"));
		Assertions.assertThat(companies)
				.extracting(Company::getId)
				.containsExactly(2, 4, 7, 1, 5, 6, 3);

		companies = companyRepository.findAll(toSort("code,desc"));
		Assertions.assertThat(companies)
				.extracting(Company::getId)
				.containsExactly(7, 1, 5, 6, 3, 2, 4);
	}

	@Test
	void testSortIgnoreCaseOnNonStringField() {
		List<Company> companies = companyRepository.findAll(toSort("id,desc,ic"));
		Assertions.assertThat(companies)
				.extracting(Company::getId)
				.containsExactly(7, 6, 5, 4, 3, 2, 1);
	}

	@Test
	@Transactional // required to get values from lazy collections
	void testSortByPropertyWithOneToManyRelation() {
		List<Company> companies = companyRepository.findAll(toSort("users.userRoles.role.code,asc;id,asc"));

		Assertions.assertThat(companies)
			.extracting(
				company -> company.getUsers().get(0).getUserRoles().get(0).getRole().getCode(),
				Company::getId
			)
			// NOTE: in this case we got duplicates for company
			// spring-data pending issue when applying sorting for nested collection property
			// https://github.com/spring-projects/spring-data-jpa/issues/1115
			// Edit: 19-12-2022: I think the above has been fixed here: https://github.com/perplexhub/rsql-jpa-specification/pull/66
			.containsExactly(
				Tuple.tuple("admin", 5),
				Tuple.tuple("user", 1),
				Tuple.tuple("user", 2),
				Tuple.tuple("user", 3),
				Tuple.tuple("user", 4)
			);
	}

	@Test
	void testFindingUserByProjectImplAttribute() {
		Specification<User> specification = RSQLJPASupport.toSpecification("projects.departmentName==someDepartmentName", true);
		List<User> foundUsers = userRepository.findAll(specification);
		Assertions.assertThat(foundUsers).extracting(User::getId).containsExactly(1);
	}

	@Test
	void testThrowUnknownPropertyException() {
		Specification<User> spec = RSQLJPASupport.toSpecification("abc==1");

		assertThatExceptionOfType(UnknownPropertyException.class)
				.isThrownBy(() -> userRepository.findAll(spec))
				.satisfies(e -> {
					assertEquals("abc", e.getName());
					assertEquals(User.class, e.getType());
				});
	}

	@Test
	@Transactional
	void testStrictEquality() {
		resetDBBeforeTest();

		User user = new User();
		user.setName("^^^ Zoe ***");
		userRepository.save(user);

		List<User> result = userRepository.findAll(toSpecification(QuerySupport.builder()
				.strictEquality(true)
				.rsqlQuery("name=='^^^ Zoe ***'")
				.build()));
		
		assertEquals(1, result.size());
		assertEquals(result.get(0).getId(), user.getId());
	}

	@Test
	@Transactional
	void testStrictEqualityNotEqual() {
		resetDBBeforeTest();

		User user1 = new User();
		user1.setName("Zoe");
		
		User user2 = new User();
		user2.setName("*Zoe");

		userRepository.deleteAll();
		userRepository.saveAll(List.of(user1, user2));

		Specification<User> spec = toSpecification(QuerySupport.builder()
				.strictEquality(true)
				.rsqlQuery("name!='*Zoe'")
				.build());

		List<User> result = userRepository.findAll(spec);

		Assertions.assertThat(result)
				.extracting(User::getId)
				.doesNotContain(user2.getId())
				.contains(user1.getId());
	}

	@Test
	@Transactional
	void testFetch() {
		Specification<User> spec = RSQLJPASupport.<User>toSpecification("company.name=ilike='inc'", true)
				.and(toSort("city.name,asc;company.name,asc;name,asc"));

		List<User> result = userRepository.findAll((root, query, builder) -> {
			/*
                 Join fetch should be applied only for query to fetch the "data", not for "count" query to do pagination.
                 Handled this by checking the criteriaQuery.getResultType(), if it's long that means query is
                 for count so not appending join fetch else append it.
              */
			if (Long.class != query.getResultType()) {
				root.fetch("company", JoinType.INNER);
				root.fetch("city", JoinType.LEFT);
				root.fetch("userRoles", JoinType.LEFT);
			}

			Predicate rsqlPredicate = spec.toPredicate(root, query, builder);

			Predicate extraPredicate = builder.equal(root.get("status"), Status.ACTIVE);

			return builder.and(rsqlPredicate, extraPredicate);
		});

		assertEquals(1, result.size());
		Assertions.assertThat(result)
				.extracting(User::getId)
				.containsExactly(6);
	}

	void resetDBBeforeTest() {
		userRepository.deleteAll();
		final int size = userRepository.findAll().size();
		assertEquals(0, size);
	}

	@BeforeEach
	void setUp() {
		getPropertyWhitelist().clear();
		getPropertyBlacklist().clear();
	}

}
