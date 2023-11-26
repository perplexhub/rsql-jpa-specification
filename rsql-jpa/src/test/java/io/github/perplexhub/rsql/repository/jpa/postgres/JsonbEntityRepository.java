package io.github.perplexhub.rsql.repository.jpa.postgres;

import io.github.perplexhub.rsql.model.JsonbEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JsonbEntityRepository extends JpaRepository<JsonbEntity, UUID> {
}

