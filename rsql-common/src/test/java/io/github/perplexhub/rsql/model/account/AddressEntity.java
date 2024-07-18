package io.github.perplexhub.rsql.model.account;

import jakarta.persistence.Embeddable;


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String value) {
        this.address = value;
    }
}
