package io.github.perplexhub.rsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
