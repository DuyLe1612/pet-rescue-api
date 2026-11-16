package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.followup.FollowupAlertDto;
import com.uit.petrescueapi.application.dto.followup.FollowupDto;
import com.uit.petrescueapi.application.dto.followup.FollowupFormDto;
import com.uit.petrescueapi.application.dto.followup.FollowupFormDto.Option;
import com.uit.petrescueapi.application.dto.followup.FollowupFormDto.Question;
import com.uit.petrescueapi.application.port.query.FollowupQueryPort;
import com.uit.petrescueapi.domain.repository.FollowupRepository.AlertRow;
import com.uit.petrescueapi.domain.repository.FollowupRepository.Row;
import com.uit.petrescueapi.domain.service.FollowupDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowupQueryUseCase implements FollowupQueryPort {

    /** The same wording at every milestone, so the answers can be compared. */
    private static final List<Question> QUESTIONS = List.of(
            new Question("stillHasPet", "Bé còn ở với bạn không?",
                    List.of(new Option("true", "Có"), new Option("false", "Không"))),
            new Question("wellbeing", "Bé ăn, ngủ, đi vệ sinh thế nào?",
                    List.of(new Option("NORMAL", "Bình thường"), new Option("WORRIED", "Hơi lo"), new Option("ALARMING", "Đáng lo"))),
            new Question("fear", "Bé có sợ hãi, cắn phá, hoặc khó ở một mình không?",
                    List.of(new Option("NONE", "Không"), new Option("SOME", "Một chút"), new Option("MUCH", "Nhiều"))),
            new Question("incident", "Có sự cố cắn hay tấn công người hoặc vật nuôi không?",
                    List.of(new Option("NONE", "Không"), new Option("NO_INJURY", "Có, không bị thương"), new Option("INJURY", "Có, bị thương"))),
            new Question("support", "Bạn có cần hỗ trợ không?",
                    List.of(new Option("NONE", "Không"), new Option("ADVICE", "Muốn được tư vấn"),
                            new Option("CONSIDERING_RETURN", "Đang cân nhắc không nuôi nữa"))));

    private static final Map<String, String> INTRO = Map.of(
            "DAY_2", "Những ngày đầu thường là lúc khó nhất: nhiều bé cần thời gian để làm quen, và điều đó hoàn toàn bình thường.",
            "WEEK_2", "Hai tuần rồi. Bé đã đi khám thú y chưa? Nếu chưa, đây là lúc nên đi.",
            "MONTH_3", "Ba tháng rồi. Chúng mình muốn biết bé và bạn đã quen nhau đến đâu.",
            "MONTH_6", "Nửa năm rồi. Cảm ơn bạn đã cho bé một mái nhà.");

    private final FollowupDomainService followups;

    @Override
    public FollowupFormDto formFor(String token) {
        Row row = followups.formFor(token);
        return new FollowupFormDto(row.petName(), row.milestone(), INTRO.get(row.milestone()), QUESTIONS);
    }

    @Override
    public List<FollowupAlertDto> alerts(UUID userId, String status) {
        return followups.alerts(userId, status).stream().map(FollowupQueryUseCase::toDto).toList();
    }

    static FollowupAlertDto toDto(AlertRow a) {
        return new FollowupAlertDto(a.alertId(), a.applicationId(), a.petId(), a.petName(), a.severity(), a.reasons(),
                a.status(), a.assignedTo(), a.createdAt(), a.firstActionAt(), a.resolvedAt(), a.resolution(), a.note());
    }

    @Override
    public List<FollowupDto> timeline(UUID applicationId, UUID viewerId) {
        return followups.timeline(applicationId, viewerId).stream()
                .map(r -> new FollowupDto(r.followupId(), r.milestone(), r.status(), r.dueAt(), r.sentAt(), r.respondedAt(),
                        r.expiresAt(), r.severity(), r.stillHasPet(), r.answers()))
                .toList();
    }
}
