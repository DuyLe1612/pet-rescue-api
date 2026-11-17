package com.uit.petrescueapi.domain.valueobject;

/** What a case is about. Only a RESCUE is claimed, completed and goes through RESCUED. */
public enum RescueCaseType {
    /** An animal in trouble that needs someone to come. */
    RESCUE,
    /** An owner looking for their pet. */
    LOST,
    /** Someone who saw or picked up an animal that looks owned. */
    FOUND;

    /** Where the point would lead to someone's home, the public sees a cell of about 300 m instead. */
    public boolean blursLocation(HoldingStatus holdingStatus) {
        return this == LOST || (this == FOUND && holdingStatus == HoldingStatus.AT_HOME);
    }
}
