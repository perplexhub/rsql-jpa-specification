package io.github.perplexhub.rsql.model.account;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
public class AccountEntity {

    @Id
    private String ident;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "account")
    private List<AddressHistoryEntity> addressHistory = new ArrayList<>();

    public AccountEntity(String ident) {
        this.ident = ident;
    }

    public AccountEntity() {
    }

}
