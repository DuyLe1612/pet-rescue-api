package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.notification.NotificationResponseDto;
import com.uit.petrescueapi.application.port.command.NotificationCommandPort;
import com.uit.petrescueapi.application.port.query.NotificationQueryPort;
import com.uit.petrescueapi.domain.service.NotificationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationUseCase implements NotificationCommandPort, NotificationQueryPort {

    private final NotificationDomainService domainService;

    @Override
    public void markRead(UUID notificationId, UUID userId) {
        domainService.markRead(notificationId, userId);
    }

    @Override
    public int markAllRead(UUID userId) {
        return domainService.markAllRead(userId);
    }

    @Override
    public Page<NotificationResponseDto> findMine(UUID userId, boolean unreadOnly, Pageable pageable) {
        return domainService.findForUser(userId, unreadOnly, pageable).map(NotificationResponseDto::from);
    }

    @Override
    public long countUnread(UUID userId) {
        return domainService.countUnread(userId);
    }
}
