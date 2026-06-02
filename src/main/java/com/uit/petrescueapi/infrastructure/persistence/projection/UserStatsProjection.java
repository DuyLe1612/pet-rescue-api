package com.uit.petrescueapi.infrastructure.persistence.projection;

public interface UserStatsProjection {
    long getTotalUsers();
    long getActiveUsers();
    long getPendingUsers();
    long getBannedUsers();
}
