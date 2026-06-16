package roomescape.reservationtime.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.ErrorCode;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;
import roomescape.reservationtime.dto.TimeRequest;
import roomescape.reservationtime.dto.TimeResponse;
import roomescape.reservationtime.service.ReservationTimeService;

@Tag(name = "예약 시간", description = "예약 시간 생성·조회·삭제 API")
@RestController
@RequestMapping("/times")
public class ReservationTimeController {

    private final ReservationTimeService reservationTimeService;

    public ReservationTimeController(ReservationTimeService reservationTimeService) {
        this.reservationTimeService = reservationTimeService;
    }

    @PostMapping
    public ResponseEntity<TimeResponse> createTime(@Valid @RequestBody TimeRequest request,
                                                     HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        TimeResponse response = reservationTimeService.createTime(request);
        return ResponseEntity.created(URI.create("/times/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TimeResponse>> getTimes() {
        return ResponseEntity.ok(reservationTimeService.getAllTimes());
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeResponse>> getAvailableTimes(
            @RequestParam LocalDate date, @RequestParam Long themeId
    ) {
        return ResponseEntity.ok(reservationTimeService.getAvailableTimes(date, themeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTime(@PathVariable Long id, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        reservationTimeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        Member member = session == null ? null : (Member) session.getAttribute("member");
        if (member == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        if (member.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.ADMIN_ACCESS_REQUIRED);
        }
    }
}