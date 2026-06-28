package com.omnixys.address.exception;

import com.omnixys.commons.error.BaseOmnixysException;
import com.omnixys.commons.error.ErrorCode;

import java.util.UUID;

public class AddressNotFoundException extends BaseOmnixysException {

    public AddressNotFoundException(UUID id) {
        super(ErrorCode.VALIDATION_ERROR, "UserAddress not found with id: " + id);
    }
}
