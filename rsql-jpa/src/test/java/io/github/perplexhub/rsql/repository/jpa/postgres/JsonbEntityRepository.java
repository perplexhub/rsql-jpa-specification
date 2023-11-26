package io.github.perplexhub.rsql.repository.jpa.postgres;

import io.github.perplexhub.rsql.model.EntityWithJsonb;
import io.github.perplexhub.rsql.model.JsonbEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface JsonbEntityRepository extends JpaRepository<JsonbEntity, UUID> {
}

