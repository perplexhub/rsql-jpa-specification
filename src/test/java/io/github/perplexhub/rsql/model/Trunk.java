package io.github.perplexhub.rsql.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Trunk {

	@Id
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "siteId", nullable = false)
	private Site site;
}
