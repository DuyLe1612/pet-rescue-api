package com.uit.petrescueapi.application.dto.matching;

import java.util.List;
import java.util.UUID;

/**
 * What the pets a person already keeps say about the "pets at home" part of their adopter profile.
 * It is a suggestion to show beside the form: the person confirms or corrects it, and a pet kept
 * but not registered here is simply not counted.
 */
public record AdopterProfileSuggestionDto(boolean hasDog, boolean hasCat, List<UUID> basedOnPetIds) {
}
