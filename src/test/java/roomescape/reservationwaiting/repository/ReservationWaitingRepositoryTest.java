package roomescape.reservationwaiting.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.ErrorCode;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.repository.JdbcReservationRepository;
import roomescape.reservation.repository.ReservationRepository;
import roomescape.reservationwaiting.domain.ReservationWaiting;
import roomescape.reservationwaiting.domain.ReservationWaitingFactory;

@JdbcTest
@Import({JdbcReservationWaitingRepository.class, JdbcReservationRepository.class, ReservationWaitingFactory.class})
@Sql(scripts = {"/truncate.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ReservationWaitingRepositoryTest {

    @Autowired
    private ReservationWaitingRepository reservationWaitingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationWaitingFactory reservationWaitingFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Reservation pastReservation;
    private Reservation futureReservation1;
    private Reservation futureReservation2;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('10:00', '11:00')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마A', '설명A', 'https://a.com')");
        Member owner = insertMember("owner", "예약자");

        jdbcTemplate.update("INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, 1, 1)",
                owner.getId(), LocalDate.now().minusDays(1));
        Long pastReservationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservation", Long.class);
        pastReservation = reservationRepository.findById(pastReservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        jdbcTemplate.update(
                "INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, '2099-12-01', 1, 1)",
                owner.getId());
        Long futureReservationId1 = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservation", Long.class);
        futureReservation1 = reservationRepository.findById(futureReservationId1)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        jdbcTemplate.update(
                "INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, '2099-12-02', 1, 1)",
                owner.getId());
        Long futureReservationId2 = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservation", Long.class);
        futureReservation2 = reservationRepository.findById(futureReservationId2)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private Member insertMember(String loginId, String name) {
        jdbcTemplate.update(
                "INSERT INTO member (login_id, name, password, role) VALUES (?, ?, 'password', 'USER')",
                loginId, name);
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM member", Long.class);
        return Member.restore(id, loginId, name, "password", Role.USER);
    }

    @Test
    @DisplayName("예약 대기 신청에 성공한다.")
    void 예약_대기_성공() {
        Member waiter = insertMember("waiter1", "현미밥");
        ReservationWaiting saved = reservationWaitingRepository.save(
                reservationWaitingFactory.create(waiter, futureReservation1));
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("이미 지난 시간과 날짜에 대해서는 대기를 신청할 수 없다")
    void 예약_대기_실패() {
        Member waiter = insertMember("waiter1", "현미밥");
        assertThatThrownBy(() -> reservationWaitingFactory.create(waiter, pastReservation))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("예약 대기 삭제한다.")
    void 예약_대기_삭제() {
        Member waiter = insertMember("waiter1", "현미밥");
        ReservationWaiting saved = reservationWaitingRepository.save(
                reservationWaitingFactory.create(waiter, futureReservation1));
        reservationWaitingRepository.deleteById(saved.getId());
        assertThat(reservationWaitingRepository.findByMemberId(waiter.getId())).hasSize(0);
    }

    @Test
    @DisplayName("회원으로 예약 대기 목록을 조회한다.")
    void 예약_대기_findByMemberId() {
        Member waiter = insertMember("waiter1", "현미밥");
        reservationWaitingRepository.save(reservationWaitingFactory.create(waiter, futureReservation1));
        reservationWaitingRepository.save(reservationWaitingFactory.create(waiter, futureReservation2));

        List<ReservationWaiting> result = reservationWaitingRepository.findByMemberId(waiter.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMember().getName()).isEqualTo("현미밥");
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2099, 12, 1));
    }

    @Test
    @DisplayName("대기 순번을 계산한다.")
    void 예약_대기_calculateTurn() {
        Member waiter1 = insertMember("waiter1", "현미밥1");
        Member waiter2 = insertMember("waiter2", "현미밥2");
        Member waiter3 = insertMember("waiter3", "현미밥3");
        reservationWaitingRepository.save(reservationWaitingFactory.create(waiter1, futureReservation1));
        ReservationWaiting waiting2 = reservationWaitingRepository.save(
                reservationWaitingFactory.create(waiter2, futureReservation1));
        reservationWaitingRepository.save(reservationWaitingFactory.create(waiter3, futureReservation1));

        Map<Long, Long> turns = reservationWaitingRepository.calculateTurn(waiter2.getId());
        assertThat(turns.get(waiting2.getId())).isEqualTo(2L);
    }

    @Test
    @DisplayName("date, themeId, timdId로 대기 객체를 받아온다.")
    void 예약_대기_조회() {
        Member waiter = insertMember("waiter1", "현미밥1");
        reservationWaitingRepository.save(reservationWaitingFactory.create(waiter, futureReservation1));
        Optional<ReservationWaiting> waiting = reservationWaitingRepository.findOldestBySlot(
                futureReservation1.getSlot());
        assertThat(waiting).isPresent();
        assertThat(waiting.get().getMember().getName()).isEqualTo("현미밥1");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(
                    LocalDate.now().atTime(14, 0).atZone(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault()
            );
        }
    }
}
