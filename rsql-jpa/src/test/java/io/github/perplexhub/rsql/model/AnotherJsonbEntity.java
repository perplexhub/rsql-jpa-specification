package io.github.perplexhub.rsql.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AnotherJsonbEntity {

  @Id
  private UUID id;

  @JdbcTypeCode(SqlTypes.JSON)
  private String data;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private String other;

  @Column(columnDefinition = "jsonb generated always as (data) stored", insertable = false, updatable = false)
  private String generated;

  @JdbcTypeCode(SqlTypes.JSON)
  @Formula("jsonb_set(data, '{f}','\"r\"', true)")
  private String formula;
}
