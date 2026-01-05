package io.github.perplexhub.rsql.repository.jpa.postgres;

import io.github.perplexhub.rsql.model.AnotherJsonbEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AnotherJsonbEntityRepository extends JpaRepository<AnotherJsonbEntity, UUID>,
        JpaSpecificationExecutor<AnotherJsonbEntity> {
}