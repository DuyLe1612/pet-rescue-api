package com.uit.petrescueapi.application.dto.admin.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.uit.petrescueapi.infrastructure.config.FlexibleLocalDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingTaskItemDto implements Serializable {
    private String taskId;
    private String type;
    private String title;
    private String referenceId;
    private String status;
    private String priority;
    
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;
    
    private String actionUrl;
}
