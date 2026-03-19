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

    @Query("""
SELECT new com.omnixys.address.models.payload.EventAddressPayload(
    ea.id,
    ea.eventId,
    c.name,
    s.name,
    ci.name,
    pc.code,
    st.name,
    hn.number,
    ea.additionalInfo
)
FROM EventAddress ea
LEFT JOIN Country c ON c.id = ea.countryId
LEFT JOIN State s ON s.id = ea.stateId
LEFT JOIN City ci ON ci.id = ea.cityId
LEFT JOIN PostalCode pc ON pc.id = ea.postalCodeId
LEFT JOIN Street st ON st.id = ea.streetId
LEFT JOIN HouseNumber hn ON hn.id = ea.houseNumberId
WHERE ea.eventId = :eventId
""")
    List<EventAddressPayload> findPayloadByEventId(UUID eventId);

}
