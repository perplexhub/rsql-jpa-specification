package io.github.perplexhub.rsql.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import io.github.perplexhub.rsql.model.UserRoleGrant;

public interface UserRoleGrantRepository extends JpaRepository<UserRoleGrant, Integer>, JpaSpecificationExecutor<UserRoleGrant> {

}
