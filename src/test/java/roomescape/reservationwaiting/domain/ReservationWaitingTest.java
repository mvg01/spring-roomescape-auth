package roomescape.reservationwaiting.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.common.domain.ReservationSlot;
import roomescape.common.exception.BusinessException;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;
import roomescape.reservation.domain.Reservation;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

class ReservationWaitingTest {
    private final Clock clock = Clock.fixed(
            LocalDate.now().atTime(14, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );
    private final ReservationWaitingFactory factory = new ReservationWaitingFactory(clock);
    private final ReservationTime time = ReservationTime.restore(1L, LocalTime.of(10, 0), LocalTime.of(11, 0));
    private final Theme theme = Theme.restore(1L, "테마1", "설명", "https://image.com");
    private final Member member = Member.restore(1L, "login1", "현미밥", "password", Role.USER);
    private final LocalDate futureDate = LocalDate.now().plusDays(1);
    private final LocalDate pastDate = LocalDate.now().plusDays(-1);
    private final Reservation reservation = Reservation.restore(1L, member,
            new ReservationSlot(futureDate, time, theme));
    private final Reservation pastreservation = Reservation.restore(1L, member,
            new ReservationSlot(pastDate, time, theme));

    @Test
    @DisplayName("대기자가 null이면 예외 발생")
    void 대기자_null_예외() {
        assertThatThrownBy(() -> factory.create(null, reservation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("대기자는 필수입니다.");
    }

    @Test
    @DisplayName("과거 예약이면 예외 발생")
    void 과거_날짜_예외() {
        assertThatThrownBy(() -> factory.create(member, pastreservation))
                .isInstanceOf(BusinessException.class);
    }
}
