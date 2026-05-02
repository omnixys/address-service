package com.omnixys.address.resolvers;

import com.omnixys.address.models.payload.AddressAutocompletePayload;
import com.omnixys.address.models.payload.GeoLocationInfo;
import com.omnixys.address.services.CountryService;
import com.omnixys.address.services.GeoapifyAutocompleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AddressAutocompleteQueryResolver {

    private final GeoapifyAutocompleteService autocompleteService;

    @QueryMapping
    public List<AddressAutocompletePayload> addressAutocomplete(
            @Argument String text,
            @Argument String countryCode,
            @Argument Integer limit
    ) {

        log.debug("GraphQL addressAutocomplete called: text='{}'", text);

        return autocompleteService.autocomplete(text, countryCode, limit);
    }

    @QueryMapping
    public GeoLocationInfo getGeoLocationInfo(
            @Argument String text,
            @Argument String countryCode,
            @Argument Integer limit
    ) {

        if (log.isDebugEnabled()) {
            log.debug("GraphQL addressAutocomplete called: text='{}'", text);
        }

        return autocompleteService.getGeoLocationInfo(text, countryCode, limit);
    }
    }