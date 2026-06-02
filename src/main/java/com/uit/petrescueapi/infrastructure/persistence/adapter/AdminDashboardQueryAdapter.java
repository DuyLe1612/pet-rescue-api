package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.admin.DashboardOverviewDto;
import com.uit.petrescueapi.application.dto.admin.mapper.DashboardMapper;
import com.uit.petrescueapi.application.port.out.DashboardDataPort;
import com.uit.petrescueapi.infrastructure.persistence.repository.OrganizationJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardQueryAdapter
        implements DashboardDataPort {

    private final UserJpaRepository userRepo;
    private final PetJpaRepository petRepo;
    private final OrganizationJpaRepository orgRepo;
    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardOverviewDto getOverview() {

        var users = userRepo.getStats();
        var pets = petRepo.getStats();
        var orgs = orgRepo.getStats();

        return dashboardMapper.toDto(users,pets,orgs);
    }
}

