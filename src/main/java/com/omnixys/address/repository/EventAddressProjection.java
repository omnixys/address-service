package com.omnixys.address.repository;

import java.util.UUID;

/**
 * Type-safe view of the native event-address query.
 */
public interface EventAddressProjection {

    UUID getId();

    UUID getEventId();

    String getCountry();

    String getState();

    String getCity();

    String getPostalCode();

    String getStreet();

    String getHouseNumber();

    String getAdditionalInfo();

    Double getLat();

    Double getLon();
}
