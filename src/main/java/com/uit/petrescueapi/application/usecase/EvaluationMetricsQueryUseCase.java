package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.admin.EvaluationMetricsDto;
import com.uit.petrescueapi.application.port.out.EvaluationMetricsDataPort;
import com.uit.petrescueapi.application.port.query.EvaluationMetricsQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluationMetricsQueryUseCase implements EvaluationMetricsQueryPort {

    private final EvaluationMetricsDataPort data;

    @Override
    public EvaluationMetricsDto.Matching matching() {
        return data.matching();
    }

    @Override
    public EvaluationMetricsDto.Rescue rescue() {
        return data.rescue();
    }
}
