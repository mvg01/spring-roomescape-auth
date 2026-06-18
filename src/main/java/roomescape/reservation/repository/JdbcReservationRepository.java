package roomescape.reservation.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.common.domain.ReservationSlot;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.dto.ReservationIdResponse;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

@Repository
public class JdbcReservationRepository implements ReservationRepository {

    private static final String BASE_QUERY = """
            SELECT r.id as reservation_id, r.date,
                   m.id as member_id, m.login_id as member_login_id, m.name as member_name,
                   m.password as member_password, m.role as member_role,
                   rt.id as time_id, rt.start_at as time_start_at, rt.finish_at as time_finish_at,
                   t.id as theme_id, t.name as theme_name, t.description as theme_description, t.image_url as theme_image_url,
                   t.store_id as theme_store_id
            FROM reservation r
            JOIN member m ON r.member_id = m.id
            JOIN reservation_time rt ON r.time_id = rt.id
            JOIN theme t ON r.theme_id = t.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    private final RowMapper<Reservation> rowMapper = (resultSet, rowNum) -> Reservation.restore(
            resultSet.getLong("reservation_id"),
            Member.restore(
                    resultSet.getLong("member_id"),
                    resultSet.getString("member_login_id"),
                    resultSet.getString("member_name"),
                    resultSet.getString("member_password"),
                    Role.valueOf(resultSet.getString("member_role"))
            ),
            new ReservationSlot(
                    resultSet.getDate("date").toLocalDate(),
                    ReservationTime.restore(
                            resultSet.getLong("time_id"),
                            resultSet.getTime("time_start_at").toLocalTime(),
                            resultSet.getTime("time_finish_at").toLocalTime()
                    ),
                    Theme.restore(
                            resultSet.getLong("theme_id"),
                            resultSet.getString("theme_name"),
                            resultSet.getString("theme_description"),
                            resultSet.getString("theme_image_url"),
                            resultSet.getObject("theme_store_id", Long.class)
                    )
            )
    );

    private final RowMapper<Long> idMapper = (resultSet, rowNum) -> (
            resultSet.getLong("id")
    );

    public JdbcReservationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("reservation")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Reservation save(Reservation reservation) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("member_id", reservation.getMember().getId())
                .addValue("date", reservation.getDate())
                .addValue("time_id", reservation.getTime().getId())
                .addValue("theme_id", reservation.getTheme().getId());
        Long id = simpleJdbcInsert.executeAndReturnKey(parameters).longValue();
        return Reservation.restore(id, reservation.getMember(),
                new ReservationSlot(reservation.getDate(), reservation.getTime(),
                        reservation.getTheme()));
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String query = "SELECT * FROM (" + BASE_QUERY + ") sub WHERE sub.reservation_id = ?";
        return jdbcTemplate.query(query, rowMapper, id).stream().findFirst();
    }

    @Override
    public List<Reservation> findAll() {
        String query = "SELECT * FROM (" + BASE_QUERY + ") sub ORDER BY sub.date DESC, sub.time_start_at DESC";
        return jdbcTemplate.query(query, rowMapper);
    }

    @Override
    public List<Reservation> findByMemberId(Long memberId) {
        String query = "SELECT * FROM (" + BASE_QUERY
                + ") sub WHERE sub.member_id = ? ORDER BY sub.date DESC, sub.time_start_at DESC";
        return jdbcTemplate.query(query, rowMapper, memberId);
    }

    @Override
    public void update(Long id, ReservationSlot slot) {
        String query = "UPDATE reservation SET date = ?, time_id = ? WHERE id = ?";
        jdbcTemplate.update(query, slot.date(), slot.time().getId(), id);
    }

    @Override
    public boolean isBooked(ReservationSlot slot) {
        String sql = "SELECT COUNT(*) FROM reservation WHERE date = ? AND time_id = ? AND theme_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, slot.date(), slot.time().getId(),
                slot.theme().getId());
        return count != null && count > 0;
    }

    @Override
    public boolean isReservedBy(ReservationSlot slot, Long memberId) {
        String query = "select count(*) from reservation where member_id = ? and date = ? and time_id = ? and theme_id = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, memberId, slot.date(), slot.time().getId(),
                slot.theme().getId());
        return count != null && count > 0;
    }

    @Override
    public boolean isBookedByOther(ReservationSlot slot, Long id) {
        String query = "select count(*) from reservation where date = ? and time_id = ? and theme_id = ? and id != ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, slot.date(), slot.time().getId(),
                slot.theme().getId(), id);
        return count != null && count > 0;
    }

    @Override
    public void deleteById(Long id) {
        String query = "delete from reservation where id = ?";
        jdbcTemplate.update(query, id);
    }

    @Override
    public ReservationIdResponse findIdBySlot(LocalDate date, Long themeId, Long timeId) {
        String query = "select id from reservation where date = ? and theme_id = ? and time_id = ?";
        return ReservationIdResponse.from(jdbcTemplate.query(query, idMapper, date, themeId, timeId).getFirst());
    }
}
