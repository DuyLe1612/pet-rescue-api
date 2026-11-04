package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.notification.NotificationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationQueryPort {

    Page<NotificationResponseDto> findMine(UUID userId, boolean unreadOnly, Pageable pageable);

    long countUnread(UUID userId);
}
