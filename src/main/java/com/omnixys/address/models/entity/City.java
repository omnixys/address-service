package com.omnixys.address.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;


@Entity
@Table(
        name = "city",
        indexes = {
                @Index(name = "idx_city_state", columnList = "state_id"),
                @Index(name = "idx_city_population_not_null", columnList = "population"),
                @Index(name = "idx_city_timezone", columnList = "timezone_id"),
                @Index(name = "idx_city_parent", columnList = "parent_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class City extends BaseEntity {
    @Id
    @GeneratedValue
    private UUID id;

    // -----------------------------------
    // Relations
    // -----------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timezone_id")
    private Timezone timezone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private City parent;

    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY)
    private List<PostalCode> postalCodes;

    // -----------------------------------
    // Fields
    // -----------------------------------

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;

    private Long population;

    @Column(length = 50)
    private String type;

    private Integer level;
}

