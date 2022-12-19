package io.github.perplexhub.rsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("Design")
@Entity
@Data
@EqualsAndHashCode(callSuper=false)
public class DesignProject extends Project {

    private String companyName;
}
