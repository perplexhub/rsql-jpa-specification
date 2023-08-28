package io.github.perplexhub.rsql;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration.HibernateEntityManagerDatabaseConfiguration;
import jakarta.persistence.EntityManager;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.vendor.Database;

@Slf4j
@Configuration
@ConditionalOnClass(EntityManager.class)
@Import(HibernateEntityManagerDatabaseConfiguration.class)
public class RSQLJPAAutoConfiguration {

  @Bean
  public RSQLCommonSupport rsqlCommonSupport(Map<String, EntityManager> entityManagerMap,
      ObjectProvider<EntityManagerDatabase> entityManagerDatabaseProvider) {
    log.info("RSQLJPAAutoConfiguration.rsqlCommonSupport(entityManagerMap:{})", entityManagerMap.size());
    var entityManagerDatabase = entityManagerDatabaseProvider.getIfAvailable(() -> new EntityManagerDatabase(Map.of()));

    return new RSQLJPASupport(entityManagerMap, entityManagerDatabase.value());
  }

  @Configuration
  @ConditionalOnClass(SessionImplementor.class)
  static
  class HibernateEntityManagerDatabaseConfiguration {

    @Bean
    public EntityManagerDatabase entityManagerDatabase(ObjectProvider<EntityManager> entityManagers) {
      return entityManagers.stream()
          .map(entityManager -> {
            var sessionFactory = entityManager.unwrap(Session.class).getSessionFactory();
            var dialect = ((SessionFactoryImpl) sessionFactory).getJdbcServices().getDialect();

            return Optional.ofNullable(toDatabase(dialect))
                .map(db -> Map.entry(entityManager, db))
                .orElse(null);
          })
          .filter(Objects::nonNull)
          .collect(collectingAndThen(
              toMap(Entry::getKey, Entry::getValue, (db1, db2) -> db1, IdentityHashMap::new),
              EntityManagerDatabase::new
          ));
    }

    private Database toDatabase(Dialect dialect) {
      if (dialect instanceof PostgreSQLDialect || dialect instanceof CockroachDialect) {
        return Database.POSTGRESQL;
      } else if (dialect instanceof MySQLDialect) {
        return Database.MYSQL;
      } else if (dialect instanceof SQLServerDialect) {
        return Database.SQL_SERVER;
      } else if (dialect instanceof OracleDialect) {
        return Database.ORACLE;
      } else if (dialect instanceof DerbyDialect) {
        return Database.DERBY;
      } else if (dialect instanceof DB2Dialect) {
        return Database.DB2;
      } else if (dialect instanceof H2Dialect) {
        return Database.H2;
      } else if (dialect instanceof AbstractHANADialect) {
        return Database.HANA;
      } else if (dialect instanceof HSQLDialect) {
        return Database.HSQL;
      } else if (dialect instanceof SybaseDialect) {
        return Database.SQL_SERVER;
      }

      return null;
    }
  }

  record EntityManagerDatabase(Map<EntityManager, Database> value) {

  }
}
