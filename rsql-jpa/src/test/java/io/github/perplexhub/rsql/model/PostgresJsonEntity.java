package io.github.perplexhub.rsql.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Entity
@NoArgsConstructor
public class PostgresJsonEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> properties = new HashMap<>();

  public PostgresJsonEntity(Map<String, Object> properties) {
    this.properties = Objects.requireNonNull(properties);
  }
  
  public PostgresJsonEntity(PostgresJsonEntity other) {
    this(other.getProperties());
  }
}
