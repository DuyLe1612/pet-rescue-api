package com.uit.petrescueapi.domain.event;

import com.uit.petrescueapi.domain.entity.Notification;

import java.util.List;

/** Published in the creating transaction; delivery listens for its commit. */
public record NotificationsCreatedEvent(List<Notification> notifications) {
}
