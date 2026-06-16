package roomescape.reservationwaiting.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.ErrorCode;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;
import roomescape.reservation.dto.ReservationResponse;
import roomescape.reservationwaiting.service.ReservationWaitingService;

@RestController
@RequestMapping("/admin/waitings")
public class AdminReservationWaitingController {
    private final ReservationWaitingService reservationWaitingService;

    public AdminReservationWaitingController(ReservationWaitingService reservationWaitingService) {
        this.reservationWaitingService = reservationWaitingService;
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<ReservationResponse> approveWaiting(@PathVariable Long id, HttpServletRequest httpRequest) {
        Member member = (Member) httpRequest.getSession().getAttribute("member");
        if (member == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        if (member.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.ADMIN_ACCESS_REQUIRED);
        }
        ReservationResponse response = reservationWaitingService.approveWaiting(id);
        return ResponseEntity.created(URI.create("/reservations/" + response.id())).body(response);
    }
}
