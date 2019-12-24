package io.github.perplexhub.rsql.model;

import java.io.Serializable;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UserRole {

	@EmbeddedId
	private UserRolePK id;

	@ManyToOne
	@JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "roleId", referencedColumnName = "id", insertable = false, updatable = false)
	private Role role;

	@Embeddable
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UserRolePK implements Serializable{

		private static final long serialVersionUID = 6766541401067339305L;

		private Integer userId;

		private Integer roleId;

	}
}
