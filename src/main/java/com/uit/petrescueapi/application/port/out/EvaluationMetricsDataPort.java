package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.admin.EvaluationMetricsDto;

public interface EvaluationMetricsDataPort {

    EvaluationMetricsDto.Matching matching();

    EvaluationMetricsDto.Rescue rescue();
}
