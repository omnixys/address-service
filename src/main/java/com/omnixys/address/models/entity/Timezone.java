package com.omnixys.address.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "timezone")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Timezone extends BaseEntity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "zone_name", nullable = false, unique = true, length = 150)
    private String zoneName; // Europe/Berlin

    @Column(name = "gmt_offset", nullable = false)
    private Integer gmtOffset; // seconds

    @Column(name = "gmt_offset_name", nullable = false, length = 20)
    private String gmtOffsetName; // UTC+01:00

    @Column(length = 10)
    private String abbreviation; // CET

    @Column(name = "tz_name", length = 150)
    private String tzName; // Central European Time

    @ManyToMany(mappedBy = "timezones")
    private Set<Country> countries = new HashSet<>();
}