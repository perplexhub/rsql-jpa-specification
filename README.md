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

## 3) Obtain the Specification from RsqlJpaSpecification.rsql(filter) using RSQL syntax

```java
filter = "company.code==perplexhub"; //equal
filter = "company.code==perplex*"; //like perplex%
filter = "company.code==*hub"; //like %hub
filter = "company.code==*plex*"; //like %plex%
filter = "company.code==^*PLEX*"; //ignore case like %PLEX%
repository.findAll(RsqlJpaSpecification.rsql(filter));
repository.findAll(RsqlJpaSpecification.rsql(filter), pageable);
```

Syntax reference: [RSQL / FIQL parser](https://github.com/jirutka/rsql-parser#examples), [RSQL for JPA](https://github.com/tennaito/rsql-jpa#examples-of-rsql) and [Dynamic-Specification-RSQL](https://github.com/srigalamilitan/Dynamic-Specification-RSQL#implementation-rsql-in-services-layer)

## 4) Maven

https://oss.sonatype.org/#nexus-search;quick~io.github.perplexhub
