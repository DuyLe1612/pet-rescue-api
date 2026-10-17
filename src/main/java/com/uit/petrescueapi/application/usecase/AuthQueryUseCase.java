package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.port.out.AuthQueryDataPort;
import com.uit.petrescueapi.application.port.query.AuthQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * Query (read) use-case for Auth/User operations.
 * Thin orchestrator — delegates directly to the infrastructure query adapter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthQueryUseCase implements AuthQueryPort {

    private final AuthQueryDataPort queryDataPort;
}
