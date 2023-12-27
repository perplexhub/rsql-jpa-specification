package io.github.perplexhub.rsql.repository.jpa.custom;

import io.github.perplexhub.rsql.custom.EntityWithCustomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomTypeRepository extends JpaRepository<EntityWithCustomType, Long>, JpaSpecificationExecutor<EntityWithCustomType> {
}
