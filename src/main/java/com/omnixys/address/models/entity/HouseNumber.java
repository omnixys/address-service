package com.omnixys.address.models.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "house_number",
        indexes = {
                @Index(name = "idx_house_number_street", columnList = "street_id"),
                @Index(name = "idx_house_number_location", columnList = "location")
        }
)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString
public class HouseNumber extends BaseEntity {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "street_id", nullable = false, updatable = false)
    private Street street;

    @Column(name = "number", nullable = false, length = 20)
    private String number;

    /**
     * Represents a point location for the specific house number.
     * Stored as GEOGRAPHY(Point, 4326) in PostGIS.
     */
    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    private Point location;
}