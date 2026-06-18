package roomescape.reservationwaiting.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.common.domain.ReservationSlot;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.reservationwaiting.domain.ReservationWaiting;
import roomescape.theme.domain.Theme;

@Repository
public class JdbcReservationWaitingRepository implements ReservationWaitingRepository {

    private static final String BASE_QUERY = """
            SELECT rw.id as reservation_waiting_id, rw.date as reservation_date, rw.created_at as created_at,
                   m.id as member_id, m.login_id as member_login_id, m.name as member_name,
                   m.password as member_password, m.role as member_role,
                   rt.id as time_id, rt.start_at as time_start_at, rt.finish_at as time_finish_at,
                   t.id as theme_id, t.name as theme_name, t.description as theme_description, t.image_url as theme_image_url,
                   t.store_id as theme_store_id
            FROM reservation_waiting rw
            JOIN member m ON rw.member_id = m.id
            JOIN reservation_time rt ON rw.time_id = rt.id
            JOIN theme t ON rw.theme_id = t.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    private final RowMapper<ReservationWaiting> rowMapper = (resultSet, rowNum) ->
            ReservationWaiting.restore(
                    resultSet.getLong("reservation_waiting_id"),
                    Member.restore(
                            resultSet.getLong("member_id"),
                            resultSet.getString("member_login_id"),
                            resultSet.getString("member_name"),
                            resultSet.getString("member_password"),
                            Role.valueOf(resultSet.getString("member_role"))
                    ),
                    resultSet.getDate("reservation_date").toLocalDate(),
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
            );

    private final ResultSetExtractor<Map<Long, Long>> turnExtractor = resultSet -> {
        Map<Long, Long> turnMap = new HashMap<>();
        while (resultSet.next()) {
            turnMap.put(resultSet.getLong("id"), resultSet.getLong("turn"));
        }
        return turnMap;
    };

    public JdbcReservationWaitingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("reservation_waiting")
                .usingGeneratedKeyColumns("id")
                .usingColumns("member_id", "date", "time_id", "theme_id");
    }

    @Override
    public ReservationWaiting save(ReservationWaiting reservationWaiting) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("member_id", reservationWaiting.getMember().getId())
                .addValue("date", reservationWaiting.getDate())
                .addValue("time_id", reservationWaiting.getTime().getId())
                .addValue("theme_id", reservationWaiting.getTheme().getId());
        Long id = simpleJdbcInsert.executeAndReturnKey(parameters).longValue();
        return ReservationWaiting.restore(id, reservationWaiting.getMember(), reservationWaiting.getDate(),
                reservationWaiting.getTime(), reservationWaiting.getTheme());
    }

    @Override
    public void deleteById(Long id) {
        String query = "delete from reservation_waiting where id = ?";
        jdbcTemplate.update(query, id);
    }

    @Override
    public Map<Long, Long> calculateTurn(Long memberId) {
        String query = """
                SELECT * FROM (
                SELECT rw.id, rw.member_id, ROW_NUMBER() OVER(PARTITION BY rw.date, rw.time_id, rw.theme_id ORDER BY rw.created_at) as turn
                FROM reservation_waiting rw) sub
                WHERE sub.member_id = ?;
                """;
        return jdbcTemplate.query(query, turnExtractor, memberId);
    }

    @Override
    public List<ReservationWaiting> findByMemberId(Long memberId) {
        String query = "SELECT * FROM (" + BASE_QUERY + ") sub WHERE sub.member_id = ? ORDER BY sub.created_at";
        return jdbcTemplate.query(query, rowMapper, memberId);
    }

    @Override
    public Optional<ReservationWaiting> findById(Long reservationWaitingId) {
        String query = "SELECT * FROM (" + BASE_QUERY
                + ") sub WHERE sub.reservation_waiting_id = ? ORDER BY sub.created_at";
        return jdbcTemplate.query(query, rowMapper, reservationWaitingId).stream().findFirst();
    }

    @Override
    public Optional<ReservationWaiting> findOldestBySlot(ReservationSlot slot) {

        String query = "SELECT * FROM (" + BASE_QUERY
                + ") sub WHERE sub.reservation_date = ? AND sub.time_id = ? AND sub.theme_id = ? ORDER BY sub.created_at";
        return jdbcTemplate.query(query, rowMapper, slot.date(), slot.time().getId(), slot.theme().getId()).stream().findFirst();
    }

    @Override
    public boolean isWaitingBy(ReservationSlot slot, Long memberId) {
        String query = "select count(*) from reservation_waiting where member_id = ? and date = ? and time_id = ? and theme_id = ?";
        return jdbcTemplate.queryForObject(query, Integer.class, memberId, slot.date(), slot.time().getId(), slot.theme().getId()) >= 1;
    }
}
