package com.uit.petrescueapi.application.port.command;

import java.util.UUID;

public interface RescueTriageCommandPort {

    /**
     * Asks the model to label a reported case and stores the answer. Safe to call twice or from two
     * workers: a case already labelled, held by another worker, or out of attempts is left alone.
     * When the AI is off it does nothing at all.
     */
    void triage(UUID caseId);
}
