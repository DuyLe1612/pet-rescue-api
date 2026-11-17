package com.uit.petrescueapi.presentation.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.uit.petrescueapi.presentation.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalExceptionAdviceTest {

    @Test
    void missingRequestParameterIsClientError() {
        ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionAdvice()
                .handleMissingParameter(new MissingServletRequestParameterException("month", "int"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("month");
    }

    @Test
    void businessErrorsCarryTheirCodeSoClientsCanBranchOnIt() {
        ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionAdvice().handleBase(
                new com.uit.petrescueapi.domain.exception.ConflictException("Pet is not available for adoption", "PET_NOT_AVAILABLE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PET_NOT_AVAILABLE");
    }
}
