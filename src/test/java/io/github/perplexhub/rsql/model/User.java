package io.github.perplexhub.rsql.model;

import java.util.List;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

	@Id
	private Integer id;

	private String name;

	@ManyToOne
	@JoinColumn(name = "companyId", referencedColumnName = "id")
	private Company company;

	@OneToMany(mappedBy = "id.userId")
	private List<UserRole> userRoles;

}
