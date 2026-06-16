package roomescape.reservationwaiting.domain;

import java.time.Clock;
import org.springframework.stereotype.Component;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.ErrorCode;
import roomescape.member.domain.Member;
import roomescape.reservation.domain.Reservation;

@Component
public class ReservationWaitingFactory {

    private final Clock clock;

    public ReservationWaitingFactory(Clock clock) {
        this.clock = clock;
    }

    public ReservationWaiting create(Member member, Reservation reservation) {
        validate(member, reservation);
        return ReservationWaiting.restore(null, member, reservation.getDate(), reservation.getTime(), reservation.getTheme());
    }

    private void validate(Member member, Reservation reservation) {
        if (member == null) {
            throw new IllegalArgumentException("대기자는 필수입니다.");
        }
        if (reservation.isPast(clock)) {
            throw new BusinessException(ErrorCode.PAST_TIME_WAITING);
        }
        if (reservation.getId() == null) {
            throw new IllegalArgumentException("예약은 필수입니다.");
        }
    }
}
