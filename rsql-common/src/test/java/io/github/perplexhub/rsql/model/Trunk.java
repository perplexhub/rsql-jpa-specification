package io.github.perplexhub.rsql.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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
