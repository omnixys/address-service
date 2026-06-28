package com.omnixys.address.resolvers;

import com.omnixys.address.models.inputs.AddressValidationInput;
import com.omnixys.address.models.payload.AddressValidationPayload;
import com.omnixys.address.services.GeoapifyAddressValidationService;
import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AddressValidationQueryResolver {

    private final GeoapifyAddressValidationService validationService;

    /**
     * GraphQL query for strict address validation.
     * - valid only if Geoapify returns exactly 1 result and confidence == 1
     */
    @QueryMapping
    public AddressValidationPayload validateAddress(@Argument @Valid AddressValidationInput input) {

        log.debug("GraphQL validateAddress called: street='{}', house='{}', zip='{}', city='{}', state='{}', country='{}'",
                input != null ? input.street() : null,
                input != null ? input.houseNumber() : null,
                input != null ? input.postalCode() : null,
                input != null ? input.city() : null,
                input != null ? input.state() : null,
                input != null ? input.country() : null
        );

        var result = validationService.validateStrict(input);

        log.debug("GraphQL validateAddress result: valid={}, reason={}, confidence={}",
                result.valid(), result.reason(), result.confidence());

        return result;
    }
}