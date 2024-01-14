package io.github.perplexhub.rsql.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EntityWithJsonb {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(cascade = CascadeType.MERGE)
    private JsonbEntity jsonb;
}
