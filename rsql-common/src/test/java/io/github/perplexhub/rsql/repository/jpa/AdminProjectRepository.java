package io.github.perplexhub.rsql.repository.jpa;

import io.github.perplexhub.rsql.model.AdminProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminProjectRepository extends JpaRepository<AdminProject, Integer>, JpaSpecificationExecutor<AdminProject> {

}
