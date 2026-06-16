package roomescape.reservation.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import roomescape.common.domain.ReservationSlot;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.ErrorCode;
import roomescape.member.domain.Member;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

@Component
public class ReservationFactory {

    private final Clock clock;

    public ReservationFactory(Clock clock) {
        this.clock = clock;
    }

    public Reservation create(Member member, ReservationSlot slot) {
        validate(member, slot.date(), slot.time(), slot.theme());
        return Reservation.restore(null, member, slot);
    }

    private void validate(Member member, LocalDate date, ReservationTime time, Theme theme) {
        if (member == null) {
            throw new IllegalArgumentException("예약자는 필수입니다.");
        }
        if (date == null) {
            throw new IllegalArgumentException("날짜는 필수입니다.");
        }
        if (time == null || time.getId() == null) {
            throw new IllegalArgumentException("예약 시간은 필수입니다.");
        }
        if (LocalDateTime.of(date, time.getStartAt()).isBefore(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.PAST_TIME_CREATE);
        }
        if (theme == null) {
            throw new IllegalArgumentException("테마는 필수입니다.");
        }
    }
}
