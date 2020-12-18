package io.github.perplexhub.rsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("Administrative")
@Entity
@Data
public class AdminProject extends Project {

    private String departmentName;
}
