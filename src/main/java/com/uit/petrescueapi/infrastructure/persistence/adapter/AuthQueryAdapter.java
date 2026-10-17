package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.AuthQueryDataPort;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * Query-side adapter for Auth/User read operations.
 * Maps domain entities → DTOs for the CQRS read path.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryAdapter implements AuthQueryDataPort {

    private final UserRepository userRepository;
}
