package io.github.perplexhub.rsql;

import static io.github.perplexhub.rsql.RSQLCommonSupport.*;
import static io.github.perplexhub.rsql.RSQLJPASupport.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit4.SpringRunner;
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
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.NONE)
public class RSQLJPASupportTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TrunkGroupRepository trunkGroupRepository;

	@Test
	public final void testJoinHints() {
		// use left join by default for one to many
		String rsql = "projects.departmentName==someDepartmentName";
		List<User> users = userRepository.findAll(toSpecification(rsql, true));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));

		// change to inner join
		Map<String, JoinType> joinHints = new HashMap<String, JoinType>() {{put("User.projects", JoinType.INNER);}};
		users = userRepository.findAll(toSpecification(rsql, true,null, joinHints));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
	}

	@Test
	public final void testCustomPredicateIsNull() {
		String rsql = "city=notAssigned=''";
		RSQLCustomPredicate<String> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=notAssigned="), String.class, input -> {
			return input.getCriteriaBuilder().isNull(input.getRoot().get("city"));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11l));
	}

	@Test
	public final void testCustomPredicateBetween() {
		String rsql = "company.id=between=(2,3)";
		RSQLCustomPredicate<Long> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=between=", true), Long.class, input -> {
			return input.getCriteriaBuilder().between(input.getPath().as(Long.class), (Long) input.getArguments().get(0), (Long) input.getArguments().get(1));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));
	}

	@Test
	public final void testCustomPredicate() {
		String rsql = "createDate=dayofweek='2'";
		RSQLCustomPredicate<Long> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=dayofweek="), Long.class, input -> {
			Expression<Long> function = input.getCriteriaBuilder().function("DAY_OF_WEEK", Long.class, input.getPath());
			return input.getCriteriaBuilder().lessThan(function, (Long) input.getArguments().get(0));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2l));

		rsql = "createDate=dayofweek='3'";
		users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));
	}

	@Test
	public final void testCustomPredicateSimilar() {
		String rsql = "name=around='May'";
		RSQLCustomPredicate<String> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=around="), String.class, input -> {
			if ("May".equals(input.getArguments().get(0))) {
				return input.getPath().in(Arrays.asList("April", "May", "June"));
			}
			return input.getCriteriaBuilder().equal(input.getPath(), (String) input.getArguments().get(0));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		rsql = "name=around='June'";
		users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
	}

	@Test
	public final void testElementCollection1() {
		String rsql = "tags=='tech'";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));
	}

	@Test
	public final void testElementCollection2() {
		String rsql = "bigTags.tag=='tech'";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));
	}

	@Test
	public final void testToComplexMultiValueMap() {
		String rsql = "sites.trunks.id==2,id=na=2,company.id=='2',id=na=3,name==''";
		Map<String, MultiValueMap<String, String>> map = toComplexMultiValueMap(rsql);
		log.info("Map<String, MultiValueMap<String, String>> map:{}", map);
		long count = map.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("ComplexMultiValueMap", count, is(4l));
		assertThat("ComplexMultiValueMap", map.get("company.id").get("==").size(), is(1));
		assertThat("ComplexMultiValueMap", map.get("id").get("=null=").size(), is(2));
		assertThat("ComplexMultiValueMap", map.get("id").get("=isnull=").size(), is(2));
		assertThat("ComplexMultiValueMap", map.get("id").get("=na=").size(), is(2));
	}

	@Test
	public final void testToMultiValueMap() {
		String rsql = "sites.trunks.id==2,id==2,company.id=='2',id==3,name==''";
		MultiValueMap<String, String> map = toMultiValueMap(rsql);
		log.info("MultiValueMap<String,String> map:{}", map);
		long count = map.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("MultiValueMap", count, is(4l));
		assertThat("MultiValueMap", map.get("id").size(), is(2));
	}

	@Test
	public final void testDoubleAssociation() {
		String rsql = "sites.trunks.id==2";
		List<TrunkGroup> trunkGroups = trunkGroupRepository.findAll(toSpecification(rsql));
		long count = trunkGroups.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("DoubleAssociation", count, is(0l));
	}

	@Test
	public final void testNull() {
		String rsql = null;
		long count = userRepository.count(toSpecification(rsql));
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("Null", count, is(12l));

		count = companyRepository.count(toSpecification(rsql));
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat("Null", count, is(7l));
	}

	@Test
	public final void testEqual() {
		String rsql = "id==2";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "id=='2'";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getName(), equalTo("World Inc"));

		rsql = "company.id=='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));
		assertThat(rsql, users.get(0).getName(), equalTo("March"));
		assertThat(rsql, users.get(1).getName(), equalTo("April"));
		assertThat(rsql, users.get(2).getName(), equalTo("May"));
		assertThat(rsql, users.get(3).getName(), equalTo("June"));
		assertThat(rsql, users.get(0).getCompany().getName(), equalTo("World Inc"));

		rsql = "name==''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getCode(), equalTo("empty"));
		assertThat(rsql, companys.get(0).getName(), equalTo(""));

		rsql = "userRoles.id.roleId=='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		rsql = "userRoles.role.code=='admin'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));
	}

	@Test
	public final void testNotEqual() {
		String rsql = "id!='2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11l));

		rsql = "company.id!='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8l));
	}

	@Test
	public final void testGreaterThan() {
		String rsql = "id>'2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10l));

		rsql = "company.id=gt='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));
	}

	@Test
	public final void testGreaterThanOrEqual() {
		String rsql = "id>='2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11l));

		rsql = "company.id=ge='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10l));

		rsql = "userRoles.id.roleId>='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		rsql = "userRoles.role.id>='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));
	}

	@Test
	public final void testLessThan() {
		String rsql = "id<'2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));

		rsql = "company.id=lt='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2l));
	}

	@Test
	public final void testLessThanOrEqual() {
		String rsql = "id<='2'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2l));

		rsql = "company.id=le='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));
	}

	@Test
	public final void testIn() {
		String rsql = "id=in=('2')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));

		rsql = "company.id=in=('2')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));

		rsql = "company.id=in='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));

		rsql = "company.id=in=(2,3)";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));

		rsql = "company.id=in=('2','4')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(5l));
	}

	@Test
	public final void testNotIn() {
		String rsql = "id=out=('2')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(11l));

		rsql = "company.id=out=('2')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8l));

		rsql = "company.id=out='2'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8l));

		rsql = "company.id=out=(2,3)";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));

		rsql = "company.id=out=('2','4')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7l));
	}

	@Test
	public final void testIsNull() {
		String rsql = "name=isnull=''";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));

		rsql = "name=null=''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));

		rsql = "name=na=''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getCode(), equalTo("null"));
		assertThat(rsql, companys.get(0).getName(), is(nullValue()));
	}

	@Test
	public final void testIsNotNull() {
		String rsql = "name=isnotnull=''";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql));
		long count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));

		rsql = "name=notnull=''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));

		rsql = "name=nn=''";
		companys = companyRepository.findAll(toSpecification(rsql));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));
		assertThat(rsql, companys.get(0).getName(), is(notNullValue()));
	}

	@Test
	public final void testLike() {
		String rsql = "name=like='ber'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));

		rsql = "company.name=like='Inc'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));

		rsql = "company.name=='*Inc*'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));

		rsql = "company.name=='*Inc'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));

		rsql = "company.name=='Inc*'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(0l));
	}

	@Test
	public final void testNotLike() {
		String rsql = "name=notlike='ber'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8l));

		rsql = "company.name=notlike='Inc'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));
	}

	@Test
	public final void testEqualsIgnoreCase() {
		String rsql = "name=icase='may'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));

		rsql = "company.name=icase='fake company'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		rsql = "company.name=='^fake company'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));
	}

	@Test
	public final void testLikeIgnoreCase() {
		String rsql = "name=ilike='BER'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));

		rsql = "company.name=ilike='INC'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));

		rsql = "company.name=='^*INC*'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));

		rsql = "company.name=='^*INC'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));

		rsql = "company.name=='^INC*'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(0l));
	}

	@Test
	public final void testNotLikeIgnoreCase() {
		String rsql = "name=inotlike='BER'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(8l));

		rsql = "company.name=inotlike='INC'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(6l));
	}

	@Test
	public final void testAnd() {
		String rsql = "company.id=in=(2,5);userRoles.role.code=='admin'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		rsql = "company.id=in=(2,5) and userRoles.role.code=='admin'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));
	}

	@Test
	public final void testOr() {
		String rsql = "company.id=in=(2,5),userRoles.role.code=='admin'";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7l));

		rsql = "company.id=in=(2,5) or userRoles.role.code=='admin'";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(7l));
	}

	@Test
	public final void testBetweenDate() {
		String rsql = "createDate=bt=('2018-01-01', '2018-10-31')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10l));
	}

	@Test
	public final void testBetweenDateTime() {
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
		assertThat(rsql, count, is(11l));
		RSQLJPASupport.removeConverter(Date.class);
	}

	@Test
	public final void testBetween() {
		String rsql = "id=bt=('2', '4')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		rsql = "company.id=bt=(2,'5')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(10l));
	}

	@Test
	public final void testNotBetween() {
		String rsql = "id=nb=('2', '4')";
		List<User> users = userRepository.findAll(toSpecification(rsql));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(9l));

		rsql = "company.id=nb=(2,'5')";
		users = userRepository.findAll(toSpecification(rsql));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(2l));
	}

	@Test
	public final void testPropertyPathMapper() {
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
		assertThat(rsql, count, is(1l));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "i=='2'";
		List<Company> companys = companyRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getName(), equalTo("World Inc"));

		rsql = "ci=='2'";
		users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(4l));
		assertThat(rsql, users.get(0).getName(), equalTo("March"));
		assertThat(rsql, users.get(1).getName(), equalTo("April"));
		assertThat(rsql, users.get(2).getName(), equalTo("May"));
		assertThat(rsql, users.get(3).getName(), equalTo("June"));
		assertThat(rsql, users.get(0).getCompany().getName(), equalTo("World Inc"));

		rsql = "n==''";
		companys = companyRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = companys.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, companys.get(0).getCode(), equalTo("empty"));
		assertThat(rsql, companys.get(0).getName(), equalTo(""));

		rsql = "urir=='2'";
		users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));

		rsql = "urrc=='admin'";
		users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));
	}

	@Test(expected = DataAccessException.class)
	public final void testWhitelist() {
		addPropertyWhitelist(User.class, "id");
		addPropertyWhitelist(Role.class, "id");
		Map<String, String> propertyPathMapper = new HashMap<>();
		propertyPathMapper.put("i", "id");
		propertyPathMapper.put("urrc", "userRoles.role.code");
		String rsql = "i==2";
		List<User> users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "urrc=='admin'";
		users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));
	}

	@Test(expected = DataAccessException.class)
	public final void testBlacklist() {
		addPropertyBlacklist(User.class, "name");
		addPropertyBlacklist(Role.class, "code");
		Map<String, String> propertyPathMapper = new HashMap<>();
		propertyPathMapper.put("i", "id");
		propertyPathMapper.put("urrc", "userRoles.role.code");
		String rsql = "i==2";
		List<User> users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		long count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(1l));
		assertThat(rsql, users.get(0).getName(), equalTo("February"));

		rsql = "urrc=='admin'";
		users = userRepository.findAll(toSpecification(rsql, propertyPathMapper));
		count = users.size();
		log.info("rsql: {} -> count: {}", rsql, count);
		assertThat(rsql, count, is(3l));
	}

	@Test(expected = DataAccessException.class)
	public final void testUnknownPropertyCauseException() {
		String rsql = "i==2";
		try {
			userRepository.findAll(toSpecification(rsql));
		} catch (DataAccessException e) {
			log.info("Expected exception", e);
			throw e;
		}
	}

	@Test
	public void testWithNullSort() {
		Page<User> users = userRepository.findAll(toSort(null), PageRequest.of(0, 5));
		Assertions.assertThat(users.getContent())
			.extracting(User::getId)
			.containsExactly(1, 2, 3, 4, 5);
	}

	@Test
	public void testSortUsersByIdDesc() {
		Page<User> users = userRepository.findAll(toSort("id,desc"), PageRequest.of(0, 5));
		Assertions.assertThat(users.getContent())
			.extracting(User::getId)
			.containsExactly(12, 11, 10, 9, 8);
	}

	@Test
	public void testSortMultipleFields() {
		List<User> users = userRepository.findAll(toSort("status,desc;company.id,desc"));
		Assertions.assertThat(users)
			.extracting(User::getId)
			.containsExactly(3, 4, 5, 1, 2, 10, 11, 12, 9, 7, 8, 6);
	}

	@Test
	public void testSortAndFilter() {
		Specification<User> specification = RSQLJPASupport.<User>toSpecification("status==ACTIVE")
			.and(toSort("company.name,desc"));

		List<User> users = userRepository.findAll(specification);
		Assertions.assertThat(users)
			.extracting(User::getId)
			.containsExactly(6, 9, 7, 8);
	}

	@Test
	public void testSortWithCustomPropertyMapping() {
		Map<String, String> propertyMapping = new HashMap<String, String>(){{
			put("userStatus", "status");
			put("companyId", "company.id");
		}};
		List<User> users = userRepository.findAll(toSort("userStatus,desc;companyId,desc", propertyMapping));
		Assertions.assertThat(users)
			.extracting(User::getId)
			.containsExactly(3, 4, 5, 1, 2, 10, 11, 12, 9, 7, 8, 6);
	}

	@Test
	public void testFindingUserByProjectImplAttribute() {
		Specification specification = RSQLJPASupport.toSpecification("projects.departmentName==someDepartmentName", true);
		List<User> foundUsers = userRepository.findAll(specification);
		Assertions.assertThat(foundUsers).extracting(User::getId).containsExactly(1);
	}

	@Before
	public void setUp() throws Exception {
		getPropertyWhitelist().clear();
		getPropertyBlacklist().clear();
	}

}
