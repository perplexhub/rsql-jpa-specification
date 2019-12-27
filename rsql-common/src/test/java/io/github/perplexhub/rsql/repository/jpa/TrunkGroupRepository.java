package io.github.perplexhub.rsql.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import io.github.perplexhub.rsql.model.TrunkGroup;

public interface TrunkGroupRepository extends JpaRepository<TrunkGroup, Integer>, JpaSpecificationExecutor<TrunkGroup> {

}
