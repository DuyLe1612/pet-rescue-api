package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.RescueMedia;

import java.util.List;
import java.util.UUID;

public interface RescueCaseMediaRepository {

    RescueMedia save(RescueMedia rescueMedia);

    List<RescueMedia> findByCaseId(UUID caseId);

    void deleteByCaseId(UUID caseId);
}