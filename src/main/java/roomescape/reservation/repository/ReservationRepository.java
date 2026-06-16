package roomescape.reservation.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import roomescape.common.domain.ReservationSlot;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.dto.ReservationIdResponse;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    List<Reservation> findAll();

    List<Reservation> findByMemberId(Long memberId);

    void update(Long id, ReservationSlot slot);

    boolean isBooked(ReservationSlot slot);

    boolean isReservedBy(ReservationSlot slot, Long memberId);

    boolean isBookedByOther(ReservationSlot slot, Long id);

    void deleteById(Long id);

    ReservationIdResponse findIdBySlot(LocalDate date, Long themeId, Long timeId);
}