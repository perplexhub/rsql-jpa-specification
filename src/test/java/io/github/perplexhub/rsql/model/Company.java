package io.github.perplexhub.rsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

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
    @CollectionTable(
            name = "company_tags",
            joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_company_tag", columnNames = {"id", "tag"})
    )
    @Column(name = "tag", length = 20, nullable = false)
    private List<String> tags;

}
