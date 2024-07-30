package io.github.perplexhub.rsql.model.account;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
public class AddressHistoryEntity {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "account_ident")
    AccountEntity account;

    OffsetDateTime activeSince;

    @Embedded
    @AttributeOverride(name = "name", column = @Column(name = "invoice_name"))
    @AttributeOverride(name = "address", column = @Column(name = "invoice_address"))
    AddressEntity invoiceAddress;

    @Embedded
    @AttributeOverride(name = "name", column = @Column(name = "shipping_name"))
    @AttributeOverride(name = "address", column = @Column(name = "shipping_address"))
    AddressEntity shippingAddress;

    public AddressHistoryEntity() {
    }

    public AddressHistoryEntity(
            AccountEntity account,
            OffsetDateTime activeSince,
            AddressEntity invoiceAddress,
            AddressEntity shippingAddress) {
        this.account = account;
        this.activeSince = activeSince;
        this.invoiceAddress = invoiceAddress;
        this.shippingAddress = shippingAddress;
    }

}
