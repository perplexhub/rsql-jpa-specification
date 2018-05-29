# rsql-jpa-specification

Idea from [RSQL / FIQL parser](https://github.com/jirutka/rsql-parser), [RSQL for JPA](https://github.com/tennaito/rsql-jpa) and [Dynamic-Specification-RSQL](https://github.com/srigalamilitan/Dynamic-Specification-RSQL)
- support entities association query using Specification

## 1) Inject the EntityManager

### 1.1)

```java
@ComponentScan("com.perplexhub.rsql.jpa")
```

### 1.2)

```java
@Bean
public com.perplexhub.rsql.jpa.RsqlJpaSpecification RsqlJpaSpecification(EntityManager entityManager) {
    return new com.perplexhub.rsql.jpa.RsqlJpaSpecification(entityManager);
}
```

### 1.3)

```java
new RsqlJpaSpecification(entityManager);
```

## 2) Add JpaSpecificationExecutor to your JPA repository class

```java
package com.perplexhub.rsql.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.perplexhub.rsql.model.User;

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