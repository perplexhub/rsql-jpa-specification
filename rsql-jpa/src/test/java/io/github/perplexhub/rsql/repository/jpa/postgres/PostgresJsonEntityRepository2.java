package io.github.perplexhub.rsql.repository.jpa.postgres;

import io.github.perplexhub.rsql.model.PostgresJsonEntity;
import io.github.perplexhub.rsql.model.PostgresJsonEntity2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PostgresJsonEntityRepository2 extends JpaRepository<PostgresJsonEntity2, UUID>,
    JpaSpecificationExecutor<PostgresJsonEntity2> {
}
