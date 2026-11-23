package io.github.perplexhub.rsql.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

/**
 * Entity with a {@code @ManyToOne} to an entity whose id is an {@code @EmbeddedId} ({@link UserRole}).
 */
@Entity
public class UserRoleGrant {

	@Id
	private Integer id;

	private String description;

	@ManyToOne
	@JoinColumns({
			@JoinColumn(name = "grantUserId", referencedColumnName = "userId"),
			@JoinColumn(name = "grantRoleId", referencedColumnName = "roleId")
	})
	private UserRole userRole;

	protected UserRoleGrant() {
	}

	public UserRoleGrant(Integer id, String description, UserRole userRole) {
		this.id = id;
		this.description = description;
		this.userRole = userRole;
	}

	public Integer getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public UserRole getUserRole() {
		return userRole;
	}
}
