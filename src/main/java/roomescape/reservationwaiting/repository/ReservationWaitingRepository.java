package roomescape.reservationwaiting.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import roomescape.common.domain.ReservationSlot;
import roomescape.reservationwaiting.domain.ReservationWaiting;

public interface ReservationWaitingRepository {
    ReservationWaiting save(ReservationWaiting reservationWaiting);

    void deleteById(Long id);

    Map<Long, Long> calculateTurn(Long memberId);

    List<ReservationWaiting> findByMemberId(Long memberId);

    Optional<ReservationWaiting> findOldestBySlot(ReservationSlot slot);

    boolean isWaitingBy(ReservationSlot slot, Long memberId);

    Optional<ReservationWaiting> findById(Long id);
}
