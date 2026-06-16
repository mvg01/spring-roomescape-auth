package roomescape.reservation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.ErrorCode;
import roomescape.member.domain.Member;
import roomescape.reservation.dto.ReservationIdResponse;
import roomescape.reservation.dto.ReservationRequest;
import roomescape.reservation.dto.ReservationResponse;
import roomescape.reservation.dto.ReservationUpdateRequest;
import roomescape.reservation.service.ReservationService;

@Tag(name = "예약", description = "예약 생성·조회·수정·삭제 API")
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getReservations(HttpServletRequest request) {
        Member member = (Member) request.getSession().getAttribute("member");
        if (member == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return ResponseEntity.ok(reservationService.getReservations(member));
    }

    @GetMapping("/id")
    public ResponseEntity<ReservationIdResponse> getReservationId(@RequestParam LocalDate date,
                                                                  @RequestParam Long themeId,
                                                                  @RequestParam Long timeId) {
        return ResponseEntity.ok(reservationService.getReservationId(date, themeId, timeId));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request,
                                                                 HttpServletRequest httpRequest) {
        Member member = (Member) httpRequest.getSession().getAttribute("member");
        if (member == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        ReservationResponse response = reservationService.createReservation(request, member);
        return ResponseEntity.created(URI.create("/reservations/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getSession().getAttribute("member");
        if (member == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return ResponseEntity.ok(reservationService.updateReservation(id, request, member));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id, HttpServletRequest httpRequest) {
        Member member = (Member) httpRequest.getSession().getAttribute("member");
        if (member == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        reservationService.deleteReservation(id, member);
        return ResponseEntity.noContent().build();
    }
}