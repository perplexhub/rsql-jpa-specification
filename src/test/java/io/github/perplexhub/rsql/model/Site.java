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
public class Site {

	@Id
	private Integer id;

	@OneToMany(mappedBy = "site", cascade = CascadeType.PERSIST)
	private List<Trunk> trunks;

	@ManyToOne
	@JoinColumn(name = "trunkGroupId", nullable = false)
	private TrunkGroup trunkGroup;

}
