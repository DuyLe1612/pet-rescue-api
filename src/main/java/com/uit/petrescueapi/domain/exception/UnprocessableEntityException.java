package com.uit.petrescueapi.domain.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends BaseException {
    public UnprocessableEntityException(String message, String errorCode) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, errorCode);
    }
}
