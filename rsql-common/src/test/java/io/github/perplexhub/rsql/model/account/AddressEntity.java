package io.github.perplexhub.rsql.model.account;

import io.github.perplexhub.rsql.model.City;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Embeddable
public class AddressEntity {

    private String name;
    private String address;

    @ManyToOne
    private City city;

    public AddressEntity(String name, String address, City city) {
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public AddressEntity(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public AddressEntity() {
    }

}
