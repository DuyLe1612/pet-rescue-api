package com.uit.petrescueapi.application.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMessageRequestDto {
    @NotBlank
    @Size(max = 4000)
    private String content;
}
