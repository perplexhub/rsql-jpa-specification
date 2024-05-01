# rsql-jpa-specification

[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.perplexhub/rsql?label=Release&logo=Release&server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/#nexus-search;gav~io.github.perplexhub~rsql*)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/io.github.perplexhub/rsql?label=Snapshot&logo=Snapshot&server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/#nexus-search;gav~io.github.perplexhub~rsql~~~)

[![Release Workflow Status](https://img.shields.io/github/actions/workflow/status/perplexhub/rsql-jpa-specification/release.yml?label=Release&style=plastic)](https://github.com/perplexhub/rsql-jpa-specification/actions/workflows/pull_request.yml)
[![Snapshot Workflow Status](https://img.shields.io/github/actions/workflow/status/perplexhub/rsql-jpa-specification/snapshot.yml?label=Snapshot&style=plastic)](https://github.com/perplexhub/rsql-jpa-specification/actions/workflows/release.yml)
[![PR Workflow Status](https://img.shields.io/github/actions/workflow/status/perplexhub/rsql-jpa-specification/pull_request.yml?label=Pull%20Request&style=plastic)](https://github.com/perplexhub/rsql-jpa-specification/actions/workflows/pull_request.yml)

Translate RSQL query into org.springframework.data.jpa.domain.Specification or com.querydsl.core.types.Predicate and support entities association query.

## SpringBoot 3 Support
rsql-jpa-specification supports SpringBoot 3 since version 6.x. (Contributed by [chriseteka](https://github.com/chriseteka))

For SpringBoot 2 users, please continue to use [version 5.x](https://github.com/perplexhub/rsql-jpa-specification/tree/5.x).


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

### Sample main class - Application.java

```java
package io.github.perplexhub.rsql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableJpaRepositories(basePackages = { "io.github.xxx.yyy.repository" })
@EnableTransactionManagement
@SpringBootApplication
public class Application {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

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
    <spring-boot.version>3.0.0</spring-boot.version>
    <spring-data-releasetrain.version>2022.0.0</spring-data-releasetrain.version>
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
filter = "company.code=isnull="; //is null
filter = "company.code=null="; //is null
filter = "company.code=na="; //is null
filter = "company.code=nn="; //is not null
filter = "company.code=notnull="; //is not null
filter = "company.code=isnotnull="; //is not null

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
sort = "id"; // order by id asc
sort = "id,asc"; // order by id asc
sort = "id,asc;company.id,desc"; // order by id asc, company.id desc
sort = "name,asc,ic"  // order by name ascending ignore case
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
    .and(RSQLSupport.toSort("company.name,asc,ic;user.id,desc"));

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

## Custom Value Converter

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

## Custom Operator & Predicate

```java
String rsql = "createDate=dayofweek='2'";
RSQLCustomPredicate<Long> customPredicate = new RSQLCustomPredicate<>(new ComparisonOperator("=dayofweek="), Long.class, input -> {
	Expression<Long> function = input.getCriteriaBuilder().function("ISO_DAY_OF_WEEK", Long.class, input.getPath());
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

## Escaping Special Characters in LIKE Predicate

For the `LIKE` statement in different RDBMS, the most commonly used special characters are:

### MySQL/MariaDB and PostgreSQL

* `%`: Represents any sequence of zero or more characters. For example, `LIKE '%abc'` would match any string ending with `abc`.
* `_`: Represents any single character. For example, `LIKE 'a_c'` would match a three-character string starting with `a` and ending with `c`.

### SQL Server

* `%` and `_`: Function in the same way as in MySQL/MariaDB and PostgreSQL.
* `[]`: Used to specify a set or range of characters. For instance, `LIKE '[a-c]%'` would match any string starting with `a`, `b`, or `c`.
* `^`: Used within `[]` to exclude characters. For example, LIKE '[^a-c]%' would match any string not starting with `a`, `b`, or `c`.

### Oracle:

* `%` and `_`: Function similarly to MySQL/MariaDB and PostgreSQL.
* `ESCAPE`: Allows specifying an escape character to include % or _ literally in the search. For example, `LIKE '%\_%' ESCAPE '\'` would match a string containing an underscore.

### LIKE in RSQL

To use escape character in RSQL, you must use `QuerySupport` to build the `Specification` with appropriate escape character.

```java
char escapeChar = '$';
QuerySupport query = QuerySupport.builder()
    .rsqlQuery("name=like='" + escapeChar + "%'")
    .likeEscapeCharacter(escapeChar)
    .build();
List<Company> users = companyRepository.findAll(toSpecification(query));
```

### Example

Above RSQL with default escape character `$` for searching string containing `_`:

```java
my_table.my_column=like='$_'
```

Will produce the following SQL:

```sql
SELECT * FROM my_table WHERE my_column LIKE '%$_%' ESCAPE '$'
```

## Jsonb Support with Postgresql

It's possible to make rsql queries on jsonb fields. For example, if you have a jsonb field named `data` in your entity, you can make queries like this:

```json
{
  "data": {
    "name": "demo",
    "user" : {
      "id": 1,
      "name": "demo"
    },
    "roles": [
      {
        "id": 1,
        "name": "admin"
      },
      {
        "id": 2,
        "name": "user"
      }
    ]
  }
}
```

```java
String rsql = "data.name==demo";
List<User> users = userRepository.findAll(toSpecification(rsql));
```

```java
String rsql = "data.user.id==1";
List<User> users = userRepository.findAll(toSpecification(rsql));
```

```java
String rsql = "data.roles.id==1";
List<User> users = userRepository.findAll(toSpecification(rsql));
```

The library use [jsonb_path_exists](https://www.postgresql.org/docs/current/functions-json.html) function under the hood.   
Json primitive types are supported such as 
* string
* number
* boolean
* array

### Temporal values support

Since Postgresql 13 jsonb supports temporal values with `datetime()` function.  
As Date time values are string in jsonb, you can make queries on them as well.  
You must use the [ISO 8601](https://json-schema.org/understanding-json-schema/reference/string.html#id9) format for date time values.

>If your request conform timezone pattern, the library will use `jsonb_path_exists_tz.  
>Then consider the timezone consideration of the [official documentation](https://www.postgresql.org/docs/current/functions-json.html)

## Stored procedure 

RSQL can call a stored procedure with the following syntax for both search and sort.  
In order to be authorized to call a stored procedure, it must be `whitelisted` and not `blacklisted`.  
The only way to whitelist or blacklist a stored procedure is to use the `QuerySupport` when performing the search or the `SortSupport` when performing the sort.

```java
String rsql = "@concat[greetings|#123]=='HELLO123'";
QuerySupport querySupport = QuerySupport.builder()
        .rsqlQuery(rsql)
        .procedureWhiteList(List.of("concat", "upper"))
        .build();
List<Item> companies = itemRepository.findAll(toSpecification(querySupport));
```

>Regex like expression can be used to whitelist or blacklist stored procedure.

### Syntax

A procedure must be prefixed with `@` and called with `[]` for arguments.

```
@procedure_name[arg1|arg2|...]
```

### Arguments

Arguments are separated by `|` and can be:
* constant (null, boolean, number, string), prefixed with `#`
* column name
* other procedure call

```
@procedure_name[arg1|arg2|...]
@procedure_name[column1|column2|...]
@procedure_name[@function_name[arg1|arg2|...]|column1|#textvalue|#123|#true|#false|#null]
```

For text value, since space is not supported by RSQL, you can use `\t` to replace space.


### Usage

#### Search

```java
String rsql1 = "@upper[code]==HELLO";
String rsql2 = "@concat[@upper[code]|name]=='TESTTest Lab'";
String rsql3 = "@concat[@upper[code]|#123]=='HELLO123'";
```

#### Sort

```java
String sort1 = "@upper[code],asc";
String sort2 = "@concat[@upper[code]|name],asc";
String sort3 = "@concat[@upper[code]|#123],asc";
```
