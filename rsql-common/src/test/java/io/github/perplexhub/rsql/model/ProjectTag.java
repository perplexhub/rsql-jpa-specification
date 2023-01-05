package io.github.perplexhub.rsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ProjectTag {

    @Id
    private Integer id;

    @ManyToOne(optional = true)
    @JoinColumn(name = "localTagId", referencedColumnName = "id", nullable = true)
    private LocalTag localTag;

}
