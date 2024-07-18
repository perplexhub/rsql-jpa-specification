package io.github.perplexhub.rsql.model.account;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

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

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public String getIdent() {
        return ident;
    }

    public List<AddressHistoryEntity> getAddressHistory() {
        return addressHistory;
    }

    public void setAddressHistory(List<AddressHistoryEntity> addressHistory) {
        this.addressHistory = addressHistory;
    }
}
