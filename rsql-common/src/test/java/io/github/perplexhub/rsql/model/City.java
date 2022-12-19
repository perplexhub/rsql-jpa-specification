package io.github.perplexhub.rsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class City {

    @Id
    private Integer id;

    private String name;

    @ManyToOne(optional = true)
    @JoinColumn(name = "parent_id", referencedColumnName = "id", nullable = true)
    private City parent;

}
