package com.omnixys.address.models.entity;

import com.omnixys.address.models.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserAddress extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID countryId;
    private UUID stateId;
    private UUID cityId;
    private UUID postalCodeId;
    private UUID streetId;
    private UUID houseNumberId;

    private String additionalInfo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "address_type", columnDefinition = "address.address_type", nullable = false)
    private AddressType addressType;
}
