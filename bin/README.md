# rsql-jpa-specification

Translate RSQL query to org.springframework.data.jpa.domain.Specification
- support entities association query

## 1) Import Config

```java
@Import(io.github.perplexhub.rsql.jpa.RsqlJpaSpecificationConfig.class)
```

## 2) Add JpaSpecificationExecutor to your JPA repository class

```java
package com.perplexhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.perplexhub.model.User;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
}
```

## 3) Obtain the Specification from RsqlJpaSpecification.rsql(rsqlQuery) using RSQL syntax

```java
filter = "company.code==demo"; //equal
filter = "company.code==''"; //equal to empty string
filter = "company.code==dem*"; //like dem%
filter = "company.code==*emo"; //like %emo
filter = "company.code==*em*"; //like %em%
filter = "company.code==^*EM*"; //ignore case like %EM%
filter = "company.code!=demo"; //not equal
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
```

```java
Pageable pageable = PageRequest.of(0, 5); //page 1 and page size is 5

repository.findAll(RsqlJpaSpecification.rsql(filter));
repository.findAll(RsqlJpaSpecification.rsql(filter), pageable);

repository.findAll(RsqlJpaSpecification.rsql(filter, true)); // select distinct
repository.findAll(RsqlJpaSpecification.rsql(filter, true), pageable);
```

Syntax reference: [RSQL / FIQL parser](https://github.com/jirutka/rsql-parser#examples), [RSQL for JPA](https://github.com/tennaito/rsql-jpa#examples-of-rsql) and [Dynamic-Specification-RSQL](https://github.com/srigalamilitan/Dynamic-Specification-RSQL#implementation-rsql-in-services-layer)

## 4) Maven

https://oss.sonatype.org/#nexus-search;quick~io.github.perplexhub
