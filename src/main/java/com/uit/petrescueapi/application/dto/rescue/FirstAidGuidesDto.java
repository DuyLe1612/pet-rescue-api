package com.uit.petrescueapi.application.dto.rescue;

import java.util.List;

/** Static first aid guidance shown while reporting a case. Not AI generated. */
public record FirstAidGuidesDto(String disclaimer, List<Guide> guides) {

    public record Guide(String code, String title, List<String> urgentSigns, List<String> steps, List<String> doNot) {
    }
}
