package io.github.perplexhub.rsql.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class JsonbEntity {

  @Id
  private UUID id;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private String data;
}
