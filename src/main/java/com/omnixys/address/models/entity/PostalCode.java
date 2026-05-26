package com.omnixys.address.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)

@Entity
@Table(
        name = "postal_code",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_postal",
                        columnNames = {"country_id", "city_id", "zip"}
                )
        },
        indexes = {
                @Index(name = "idx_postal_zip_country", columnList = "country_id, zip"),
                @Index(name = "idx_postal_city", columnList = "city_id"),
                @Index(name = "idx_postal_country", columnList = "country_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostalCode extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    // -----------------------------------
    // Relations
    // -----------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    // -----------------------------------
    // Fields
    // -----------------------------------

    @Column(nullable = false, length = 20)
    private String code;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;

    private Integer accuracy;
}
