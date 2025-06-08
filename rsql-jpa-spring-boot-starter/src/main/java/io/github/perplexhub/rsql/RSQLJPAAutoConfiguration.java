package io.github.perplexhub.rsql;


import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration.HibernateEntityManagerDatabaseConfiguration;
import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@ConditionalOnClass(EntityManager.class)
@Import(HibernateEntityManagerDatabaseConfiguration.class)
public class RSQLJPAAutoConfiguration {

  @Bean
  public RSQLCommonSupport rsqlCommonSupport(Map<String, EntityManager> entityManagerMap,
      ObjectProvider<EntityManagerDatabase> entityManagerDatabaseProvider) {
    log.info("RSQLJPAAutoConfiguration.rsqlCommonSupport(entityManagerMap:{})", entityManagerMap.size());
    EntityManagerDatabase entityManagerDatabase = entityManagerDatabaseProvider.getIfAvailable(() -> new EntityManagerDatabase(new HashMap()));

    return new RSQLJPASupport(entityManagerMap, entityManagerDatabase.value());
  }

  @Configuration
  @ConditionalOnClass(SessionImplementor.class)
  static
  class HibernateEntityManagerDatabaseConfiguration {

    @Transactional
    @Bean
    public EntityManagerDatabase entityManagerDatabase(ObjectProvider<EntityManager> entityManagers) {
      Map<EntityManager, Database> value = new HashMap<>();
      EntityManager entityManager = entityManagers.getIfAvailable();
      SessionFactory sessionFactory = entityManager.unwrap(Session.class).getSessionFactory();
      Dialect dialect = ((SessionFactoryImpl) sessionFactory).getJdbcServices().getDialect();

      Database db = toDatabase(dialect);
      if (db != null) {
        value.put(entityManager, db);
      }

      return new EntityManagerDatabase(value);
    }

    private Database toDatabase(Dialect dialect) {
      if (dialect instanceof PostgreSQLDialect) {
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
      } else if (dialect instanceof HSQLDialect) {
        return Database.HSQL;
      } else if (dialect instanceof SybaseDialect) {
        return Database.SQL_SERVER;
      }

      return null;
    }
  }

  public static final class EntityManagerDatabase {
      private final Map<EntityManager, Database> value;

      public EntityManagerDatabase(Map<EntityManager, Database> value) {
          this.value = value;
      }

      public Map<EntityManager, Database> value() {
          return value;
      }

      @Override
      public boolean equals(Object obj) {
          if (this == obj) return true;
          if (obj == null || getClass() != obj.getClass()) return false;
          EntityManagerDatabase that = (EntityManagerDatabase) obj;
          return Objects.equals(value, that.value);
      }

      @Override
      public int hashCode() {
          return Objects.hash(value);
      }

      @Override
      public String toString() {
          return "EntityManagerDatabase[value=" + value + "]";
      }
  }
}
