package io.github.perplexhub.rsql.model.account;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Embeddable
public class AddressEntity {

    private String name;
    private String address;

    public AddressEntity(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public AddressEntity() {
    }

}
