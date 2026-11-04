package com.uit.petrescueapi.infrastructure.notifications;

import com.uit.petrescueapi.application.dto.chat.CreateConversationRequestDto;
import com.uit.petrescueapi.application.port.command.ChatCommandPort;
import com.uit.petrescueapi.domain.event.RescueCaseClaimedEvent;
import com.uit.petrescueapi.domain.valueobject.ConversationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Opens the chat between claimer and reporter. A chat problem must not undo the claim. */
@Component
@RequiredArgsConstructor
@Slf4j
public class RescueChatListener {

    private final ChatCommandPort chat;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClaimed(RescueCaseClaimedEvent event) {
        if (event.reporterId() == null || event.reporterId().equals(event.claimerId())) {
            return;
        }
        try {
            CreateConversationRequestDto request = new CreateConversationRequestDto();
            request.setType(ConversationType.RESCUE.name());
            request.setParticipantId(event.reporterId());
            request.setRescueCaseId(event.caseId());
            chat.createConversation(request, event.claimerId());
        } catch (RuntimeException e) {
            log.warn("Could not open chat for rescue case {}: {}", event.caseId(), e.getMessage());
        }
    }
}
