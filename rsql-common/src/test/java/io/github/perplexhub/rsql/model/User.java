package io.github.perplexhub.rsql.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.*;

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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String name;

	private String number;

	private String email;

	private String phone;

	@ManyToOne
	@JoinColumn(name = "companyId", referencedColumnName = "id")
	private Company company;

	@ManyToOne(optional = true)
	@JoinColumn(name = "cityId", referencedColumnName = "id", nullable = true)
	private City city;

	@OneToMany(mappedBy = "id.userId", cascade = CascadeType.ALL)
	private List<UserRole> userRoles;

	@Temporal(TemporalType.TIMESTAMP)
	private Date createDate;

	@Enumerated(EnumType.STRING)
	private Status status = Status.STARTED;

	@JoinColumn(name = "userId")
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Project> projects;
}
