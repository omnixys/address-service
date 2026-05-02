package com.omnixys.address.repository;

import com.omnixys.address.models.entity.EventAddress;
import com.omnixys.address.models.payload.EventAddressPayload;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAddressRepository extends JpaRepository<EventAddress, UUID>, JpaSpecificationExecutor<EventAddress> {

    void deleteByEventId(UUID eventId);

    @Override
    @NonNull
    Optional<EventAddress> findById(UUID id);

//    @Query("""
//SELECT new com.omnixys.address.models.payload.EventAddressPayload(
//    ea.id,
//    ea.eventId,
//    c.name,
//    s.name,
//    ci.name,
//    pc.code,
//    st.name,
//    hn.number,
//    ea.additionalInfo,
//    1,
//    1
//)
//FROM EventAddress ea
//LEFT JOIN Country c ON c.id = ea.countryId
//LEFT JOIN State s ON s.id = ea.stateId
//LEFT JOIN City ci ON ci.id = ea.cityId
//LEFT JOIN PostalCode pc ON pc.id = ea.postalCodeId
//LEFT JOIN Street st ON st.id = ea.streetId
//LEFT JOIN HouseNumber hn ON hn.id = ea.houseNumberId
//WHERE ea.eventId = :eventId
//""")
//    EventAddressPayload findPayloadByEventId(UUID eventId);

    /**
     * Native query because PostGIS functions are required.
     */
    @Query(value = """
        SELECT
            ea.id,
            ea.event_id,
            c.name,
            s.name,
            ci.name,
            pc.code,
            st.name,
            hn.number,
            ea.additional_info,
    ST_Y(COALESCE(hn.location, st.location)::geometry) AS lat,
    ST_X(COALESCE(hn.location, st.location)::geometry) AS lon
        FROM event_address ea
        LEFT JOIN country c ON c.id = ea.country_id
        LEFT JOIN state s ON s.id = ea.state_id
        LEFT JOIN city ci ON ci.id = ea.city_id
        LEFT JOIN postal_code pc ON pc.id = ea.postal_code_id
        LEFT JOIN street st ON st.id = ea.street_id
        LEFT JOIN house_number hn ON hn.id = ea.house_number_id
        WHERE ea.event_id = :eventId
        LIMIT 1
    """, nativeQuery = true)
    Optional<Object[]> findRawByEventId(UUID eventId);
}
