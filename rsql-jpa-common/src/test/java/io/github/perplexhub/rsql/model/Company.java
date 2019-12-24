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
public class Company {

	@Id
	private Integer id;

	private String code;

	private String name;

	@ElementCollection
	@CollectionTable(name = "company_tags", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"), uniqueConstraints = @UniqueConstraint(name = "uk_company_tag", columnNames = { "id", "tag" }))
	@Column(name = "tag", length = 20, nullable = false)
	private List<String> tags;

	@ElementCollection
	@CollectionTable(name = "company_tags", joinColumns = @JoinColumn(name = "id"))
	private List<BigTag> bigTags;

}
