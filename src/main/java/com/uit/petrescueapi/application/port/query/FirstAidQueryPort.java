package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.rescue.FirstAidGuidesDto;

public interface FirstAidQueryPort {

    FirstAidGuidesDto findGuides();
}
