package io.github.perplexhub.rsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "Type")
public abstract class Project {

    @Id
    private Integer id;

    private String name;

    @ManyToOne(optional = true)
    @JoinColumn(name = "projectTagId", referencedColumnName = "id", nullable = true)
    private ProjectTag projectTag;

}
