package com.omnixys.address.models.entity;

import com.omnixys.address.models.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "event_address", schema = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventAddress extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    private UUID countryId;
    private UUID stateId;
    private UUID cityId;
    private UUID postalCodeId;
    private UUID streetId;
    private UUID houseNumberId;

    private String additionalInfo;
}
