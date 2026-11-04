package com.uit.petrescueapi.infrastructure.scheduling;

import com.uit.petrescueapi.application.port.command.RescueMaintenancePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RescueMaintenanceJob {

    private final RescueMaintenancePort maintenance;

    /** Not every minute: frequent queries would stop a free-tier database from ever suspending. */
    @Scheduled(fixedDelayString = "${app.rescue.maintenance-delay-ms:900000}",
            initialDelayString = "${app.rescue.maintenance-delay-ms:900000}")
    public void run() {
        try {
            maintenance.run();
        } catch (RuntimeException e) {
            log.warn("Rescue maintenance failed: {}", e.getMessage());
        }
    }
}
