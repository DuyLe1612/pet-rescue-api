package com.uit.petrescueapi.application.port.command;

/** Time-driven rescue work: claim reminders and expiry, overdue completion reviews. */
public interface RescueMaintenancePort {

    void run();
}
