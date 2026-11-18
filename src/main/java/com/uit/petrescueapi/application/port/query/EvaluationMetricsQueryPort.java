package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.admin.EvaluationMetricsDto;

public interface EvaluationMetricsQueryPort {

    EvaluationMetricsDto.Matching matching();

    EvaluationMetricsDto.Rescue rescue();
}
