package com.uit.petrescueapi.domain.valueobject;

/**
 * What the adopter says at a milestone. The wording of the five questions stays the same across
 * milestones so the answers can be compared. When the pet is no longer with them the other answers
 * may be missing.
 */
public record FollowupAnswers(Boolean stillHasPet, Wellbeing wellbeing, Fear fear, Incident incident, Support support, String note) {

    /** Eating, sleeping and toileting. */
    public enum Wellbeing { NORMAL, WORRIED, ALARMING }

    /** Fear, destruction, difficulty being alone. */
    public enum Fear { NONE, SOME, MUCH }

    /** A bite or attack on a person or another animal. */
    public enum Incident { NONE, NO_INJURY, INJURY }

    public enum Support { NONE, ADVICE, CONSIDERING_RETURN }
}
