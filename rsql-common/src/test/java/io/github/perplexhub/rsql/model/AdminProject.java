package io.github.perplexhub.rsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("Administrative")
@Entity
@Data
@EqualsAndHashCode(callSuper=false)
public class AdminProject extends Project {

    private String departmentName;
}
