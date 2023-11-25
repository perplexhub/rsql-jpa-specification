package io.github.perplexhub.rsql.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Entity
@NoArgsConstructor
public class PostgresJsonEntity2 {

  @Id
  @GeneratedValue
  private UUID id;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private String data;

  public PostgresJsonEntity2(String data) {
    this.data = data;
  }
}
