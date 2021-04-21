# rsql-jpa-specification

[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.perplexhub/rsql?label=Release&logo=Release&server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/#nexus-search;gav~io.github.perplexhub~rsql*)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/io.github.perplexhub/rsql?label=Snapshot&logo=Snapshot&server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/#nexus-search;gav~io.github.perplexhub~rsql~~~)

[![Release Workflow Status](https://img.shields.io/github/workflow/status/perplexhub/rsql-jpa-specification/Maven%20Release?label=Release&logo=Release)](https://github.com/perplexhub/rsql-jpa-specification/actions?query=workflow%3A%22Maven+Release%22)
[![Snapshot Workflow Status](https://img.shields.io/github/workflow/status/perplexhub/rsql-jpa-specification/Java%20CI?label=Snapshot&logo=Snapshot)](https://github.com/perplexhub/rsql-jpa-specification/actions?query=workflow%3A%22Java+CI%22)
[![PR Workflow Status](https://img.shields.io/github/workflow/status/perplexhub/rsql-jpa-specification/Java%20Pull%20Request%20CI?label=Pull+Request&logo=PR)](https://github.com/perplexhub/rsql-jpa-specification/actions?query=workflow%3A%22Java+Pull+Request+CI%22)

Translate RSQL query into org.springframework.data.jpa.domain.Specification or com.querydsl.core.types.Predicate and support entities association query.

[Supported Operators](https://github.com/perplexhub/rsql-jpa-specification/blob/master/rsql-common/src/main/java/io/github/perplexhub/rsql/RSQLOperators.java)

[Since version 5.0.5, you can define your own operators and customize the logic via RSQLCustomPredicate.](https://github.com/perplexhub/rsql-jpa-specification/blob/master/rsql-jpa/src/test/java/io/github/perplexhub/rsql/RSQLJPASupportTest.java)

## Maven Repository

<https://oss.sonatype.org/#nexus-search;gav~io.github.perplexhub~rsql*>

## Add rsql-jpa-spring-boot-starter for RSQL to Spring JPA translation

### Maven dependency for rsql-jpa-spring-boot-starter [![](https://img.shields.io/nexus/r/io.github.perplexhub/rsql-jpa-spring-boot-starter?color=black&label=%20&server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/#nexus-search;gav~io.github.perplexhub~rsql-jpa-spring-boot-starter*)

```xml
  <dependency>
    <groupId>io.github.perplexhub</groupId>
    <artifactId>rsql-jpa-spring-boot-starter</artifactId>
    <version>X.X.X</version>
  </dependency>
```

### Add JpaSpecificationExecutor to your JPA repository interface classes

```java
package com.perplexhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.perplexhub.model.User;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
}
```

## Add rsql-querydsl-spring-boot-starter for RSQL to Spring JPA and QueryDSL translation

### Maven dependency for rsql-querydsl-spring-boot-starter [![](https://img.shields.io/nexus/r/io.github.perplexhub/rsql-querydsl-spring-boot-starter?color=black&label=%20&server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/#nexus-search;gav~io.github.perplexhub~rsql-querydsl-spring-boot-starter*)

```xml
  <dependency>
    <groupId>io.github.perplexhub</groupId>
    <artifactId>rsql-querydsl-spring-boot-starter</artifactId>
    <version>X.X.X</version>
  </dependency>
```


### Add JpaSpecificationExecutor and QuerydslPredicateExecutor to your JPA repository interface classes

```java
package com.perplexhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.perplexhub.model.User;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User>, QuerydslPredicateExecutor<User> {
}
```

## Use below properties to control the version of Spring Boot, Spring Data and QueryDSL

```xml
  <properties>
    <spring-boot.version>2.0.0.RELEASE</spring-boot.version>
    <spring-data-releasetrain.version>Kay-RELEASE</spring-data-releasetrain.version>
    <querydsl.version>4.1.4</querydsl.version>
  </properties>
```

## RSQL Syntax Reference

```java
filter = "id=bt=(2,4)";// id>=2 && id<=4 //between
filter = "id=nb=(2,4)";// id<2 || id>4 //not between
filter = "company.code=like=em"; //like %em%
filter = "company.code=ilike=EM"; //ignore case like %EM%
filter = "company.code=icase=EM"; //ignore case equal EM
filter = "company.code=notlike=em"; //not like %em%
filter = "company.code=inotlike=EM"; //ignore case not like %EM%
filter = "company.code=ke=e*m"; //like %e*m%
filter = "company.code=ik=E*M"; //ignore case like %E*M%
filter = "company.code=nk=e*m"; //not like %e*m%
filter = "company.code=ni=E*M"; //ignore case not like %E*M%
filter = "company.code=ic=E^^M"; //ignore case equal E^^M
filter = "company.code==demo"; //equal
filter = "company.code=='demo'"; //equal
filter = "company.code==''"; //equal to empty string
filter = "company.code==dem*"; //like dem%
filter = "company.code==*emo"; //like %emo
filter = "company.code==*em*"; //like %em%
filter = "company.code==^EM"; //ignore case equal EM
filter = "company.code==^*EM*"; //ignore case like %EM%
filter = "company.code=='^*EM*'"; //ignore case like %EM%
filter = "company.code!=demo"; //not equal
filter = "company.code=in=(*)"; //equal to *
filter = "company.code=in=(^)"; //equal to ^
filter = "company.code=in=(demo,real)"; //in
filter = "company.code=out=(demo,real)"; //not in
filter = "company.id=gt=100"; //greater than
filter = "company.id=lt=100"; //less than
filter = "company.id=ge=100"; //greater than or equal
filter = "company.id=le=100"; //less than or equal
filter = "company.id>100"; //greater than
filter = "company.id<100"; //less than
filter = "company.id>=100"; //greater than or equal
filter = "company.id<=100"; //less than or equal
filter = "company.code=isnull=''"; //is null
filter = "company.code=null=''"; //is null
filter = "company.code=na=''"; //is null
filter = "company.code=nn=''"; //is not null
filter = "company.code=notnull=''"; //is not null
filter = "company.code=isnotnull=''"; //is not null

filter = "company.code=='demo';company.id>100"; //and
filter = "company.code=='demo' and company.id>100"; //and

filter = "company.code=='demo',company.id>100"; //or
filter = "company.code=='demo' or company.id>100"; //or
```

Syntax Reference: [RSQL / FIQL parser](https://github.com/jirutka/rsql-parser#examples)

## Spring Data JPA Specification

```java
Pageable pageable = PageRequest.of(0, 5); //page 1 and page size is 5

repository.findAll(RSQLSupport.toSpecification(filter));
repository.findAll(RSQLSupport.toSpecification(filter), pageable);

repository.findAll(RSQLSupport.toSpecification(filter, true)); // select distinct
repository.findAll(RSQLSupport.toSpecification(filter, true), pageable);

// use static import
import static io.github.perplexhub.rsql.RSQLSupport.*;

repository.findAll(toSpecification(filter));
repository.findAll(toSpecification(filter), pageable);

repository.findAll(toSpecification(filter, true)); // select distinct
repository.findAll(toSpecification(filter, true), pageable);

// property path remap
filter = "compCode=='demo';compId>100"; // "company.code=='demo';company.id>100" -  protect our domain model #10

Map<String, String> propertyPathMapper = new HashMap<>();
propertyPathMapper.put("compId", "company.id");
propertyPathMapper.put("compCode", "company.code");

repository.findAll(toSpecification(filter, propertyPathMapper));
repository.findAll(toSpecification(filter, propertyPathMapper), pageable);
```

## Sort Syntax

```java
sort = "id,asc"; // order by id asc
sort = "id,asc;company.id,desc"; // order by id asc, company.id desc
```

## Sort with JPA Specifications

```java
repository.findAll(RSQLSupport.toSort("id,asc;company.id,desc"));

// sort with custom field mapping
Map<String, String> propertyMapping = new HashMap<>();
propertyMapping.put("userID", "id");
propertyMapping.put("companyID", "company.id");

repository.findAll(RSQLSupport.toSort("userID,asc;companyID,desc", propertyMapping)); // same as id,asc;company.id,desc

```

## Filtering and Sorting with JPA Specification
```java
Specification<?> specification = RSQLSupport.toSpecification("company.name==name")
    .and(RSQLSupport.toSort("company.name,asc,user.id,desc"));

repository.findAll(specification);
```

## QueryDSL Predicate (BooleanExpression)

```java
Pageable pageable = PageRequest.of(0, 5); //page 1 and page size is 5

repository.findAll(RSQLSupport.toPredicate(filter, QUser.user));
repository.findAll(RSQLSupport.toPredicate(filter, QUser.user), pageable);

// use static import
import static io.github.perplexhub.rsql.RSQLSupport.*;

repository.findAll(toPredicate(filter, QUser.user));
repository.findAll(toPredicate(filter, QUser.user), pageable);

// property path remap
filter = "compCode=='demo';compId>100"; // "company.code=='demo';company.id>100" - protect our domain model #10

Map<String, String> propertyPathMapper = new HashMap<>();
propertyPathMapper.put("compId", "company.id");
propertyPathMapper.put("compCode", "company.code");

repository.findAll(toPredicate(filter, QUser.user, propertyPathMapper));
repository.findAll(toPredicate(filter, QUser.user, propertyPathMapper), pageable);
```

# Custom Value Converter

```java
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		RSQLJPASupport.addConverter(Date.class, s -> {
			try {
				return sdf.parse(s);
			} catch (ParseException e) {
				return null;
			}
		});
```

# Custom Operator & Predicate

```java
		String rsql = "createDate=dayofweek='2'";
		RSQLCustomPredicate<Long> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=dayofweek="), Long.class, input -> {
			Expression<Long> function = input.getCriteriaBuilder().function("DAY_OF_WEEK", Long.class, input.getPath());
			return input.getCriteriaBuilder().lessThan(function, (Long) input.getArguments().get(0));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
```

```java
		String rsql = "name=around='May'";
		RSQLCustomPredicate<String> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=around="), String.class, input -> {
			if ("May".equals(input.getArguments().get(0))) {
				return input.getPath().in(Arrays.asList("April", "May", "June"));
			}
			return input.getCriteriaBuilder().equal(input.getPath(), (String) input.getArguments().get(0));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
```

```java
		String rsql = "company.id=between=(2,3)";
		RSQLCustomPredicate<Long> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=between=", true), Long.class, input -> {
			return input.getCriteriaBuilder().between(input.getPath().as(Long.class), (Long) input.getArguments().get(0), (Long) input.getArguments().get(1));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
```

```java
		String rsql = "city=notAssigned=''";
		RSQLCustomPredicate<String> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=notAssigned="), String.class, input -> {
			return input.getCriteriaBuilder().isNull(input.getRoot().get("city"));
		});
		List<User> users = userRepository.findAll(toSpecification(rsql, Arrays.asList(customPredicate)));
```