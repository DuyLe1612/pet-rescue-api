package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.dto.chat.FriendSummaryDto;
import com.uit.petrescueapi.application.dto.comment.CursorPageDto;
import com.uit.petrescueapi.application.port.out.FriendQueryDataPort;
import com.uit.petrescueapi.application.port.query.FriendQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendQueryUseCase implements FriendQueryPort {

    private final FriendQueryDataPort friendQueryDataPort;

    @Override
    public CursorPageDto<FriendSummaryDto> listFriendsByCursor(UUID userId, LocalDateTime cursor, int size, String search) {
        log.debug("Query: list friends by cursor for user {} (search={})", userId, search);
        Page<FriendSummaryDto> page = friendQueryDataPort.listFriendsByCursor(
                userId,
                cursor,
                search,
                org.springframework.data.domain.PageRequest.of(0, size)
        );
        var items = page.getContent();
        LocalDateTime nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).getCreatedAt();
        boolean hasMore = items.size() == size;
        return CursorPageDto.<FriendSummaryDto>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Override
    public CursorPageDto<FriendRequestDto> listPendingRequestsByCursor(UUID userId, LocalDateTime cursor, int size, String search) {
        log.debug("Query: list pending requests by cursor for user {} (search={})", userId, search);
        Page<FriendRequestDto> page = friendQueryDataPort.listPendingRequestsByCursor(
                userId,
                cursor,
                search,
                org.springframework.data.domain.PageRequest.of(0, size)
        );
        var items = page.getContent();
        LocalDateTime nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).getCreatedAt();
        boolean hasMore = items.size() == size;
        return CursorPageDto.<FriendRequestDto>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}
