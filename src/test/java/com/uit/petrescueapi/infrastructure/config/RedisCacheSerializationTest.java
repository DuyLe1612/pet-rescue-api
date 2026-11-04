package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.dto.admin.dashboard.AdoptionTrendDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.AdoptionTrendItemDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.KpiItemDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.PendingTaskItemDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.PendingTasksDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.PeriodDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.RescueSummaryDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.StatusSummaryItemDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.SystemDashboardOverviewDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.SystemKpisDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.TopOrganizationItemDto;
import com.uit.petrescueapi.application.dto.admin.dashboard.TopOrganizationsDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** A cached dashboard must come back as the same type, or the second visit fails with a ClassCastException. */
class RedisCacheSerializationTest {

    final RedisSerializer<Object> serializer = new RedisConfig().cacheValueSerializer();

    @Test
    void overviewComesBackAsItself() {
        SystemDashboardOverviewDto dto = SystemDashboardOverviewDto.builder()
                .period(new PeriodDto(11, 2026))
                .kpis(SystemKpisDto.builder().totalPets(new KpiItemDto(60, 1.7, "up")).build())
                .build();

        assertThat(roundTrip(dto)).isInstanceOf(SystemDashboardOverviewDto.class).isEqualTo(dto);
    }

    @Test
    void listsMadeWithToListComeBack() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 4, 10, 0);
        Object[] values = {
                AdoptionTrendDto.builder().year(2026).chartType("line")
                        .items(Stream.of(new AdoptionTrendItemDto(1, "T1", 1, 2, 3, 4)).toList()).build(),
                RescueSummaryDto.builder().period(new PeriodDto(11, 2026))
                        .byStatus(Stream.of(new StatusSummaryItemDto("REPORTED", "Mới", 3)).toList()).build(),
                TopOrganizationsDto.builder()
                        .items(Stream.of(new TopOrganizationItemDto(UUID.randomUUID(), "Org", "ACTIVE", 1, 2, 3, 4, 0)).toList()).build(),
                PendingTasksDto.builder()
                        .items(List.of(new PendingTaskItemDto("id", "TYPE", "title", "ref", "PENDING", "HIGH", now, "/url"))).build(),
        };

        for (Object value : values) {
            assertThat(roundTrip(value)).isInstanceOf(value.getClass()).isEqualTo(value);
        }
    }

    private Object roundTrip(Object value) {
        return serializer.deserialize(serializer.serialize(value));
    }
}
