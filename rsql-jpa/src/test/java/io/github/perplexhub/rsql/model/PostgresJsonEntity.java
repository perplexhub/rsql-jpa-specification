package io.github.perplexhub.rsql.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Entity
@NoArgsConstructor
@TypeDef(name="jsonb", typeClass=JsonType.class)
public class PostgresJsonEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Type(type="jsonb")
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> properties = new HashMap<>();

  public PostgresJsonEntity(Map<String, Object> properties) {
    this.properties = Objects.requireNonNull(properties);
  }
  
  public PostgresJsonEntity(PostgresJsonEntity other) {
    this(other.getProperties());
  }
}
