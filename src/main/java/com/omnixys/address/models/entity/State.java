package com.omnixys.address.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_state_country_code",
                        columnNames = {"country_id", "code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class State extends BaseEntity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    // =====================================================
    // RELATIONS
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private State parent;

    @ManyToMany
    @JoinTable(
            name = "state_timezone",
            joinColumns = @JoinColumn(name = "state_id"),
            inverseJoinColumns = @JoinColumn(name = "timezone_id")
    )
    @Builder.Default
    private Set<Timezone> timezones = new HashSet<>();

    // =====================================================
    // FIELDS
    // =====================================================

    @Column(nullable = false, length = 20)
    private String code;              // BDS

    @Column(name = "iso3166_2", length = 20)
    private String iso3166Code;         // AF-BDS

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String type;              // province, state, region

    private Integer level;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    private Long population;

    @OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
    private List<City> cities = new ArrayList<>();
}

