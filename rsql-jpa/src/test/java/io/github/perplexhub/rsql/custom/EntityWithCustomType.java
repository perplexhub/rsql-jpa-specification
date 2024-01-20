package io.github.perplexhub.rsql.custom;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class EntityWithCustomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private CustomType custom;

    public static EntityWithCustomType of(String name, String bic) {
        EntityWithCustomType entity = new EntityWithCustomType();
        entity.setName(name);
        entity.setCustom(CustomType.of(bic));
        return entity;
    }
}