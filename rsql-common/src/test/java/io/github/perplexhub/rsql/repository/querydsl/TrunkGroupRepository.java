package io.github.perplexhub.rsql.repository.querydsl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import io.github.perplexhub.rsql.model.TrunkGroup;

public interface TrunkGroupRepository extends JpaRepository<TrunkGroup, Integer>, JpaSpecificationExecutor<TrunkGroup>, QuerydslPredicateExecutor<TrunkGroup> {

}
