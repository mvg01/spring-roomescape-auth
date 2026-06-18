package roomescape.member.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;

@Repository
public class JdbcMemberRepository implements MemberRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    private final RowMapper<Member> rowMapper = (resultSet, rowNum) -> Member.restore(
            resultSet.getLong("id"),
            resultSet.getString("login_id"),
            resultSet.getString("name"),
            resultSet.getString("password"),
            Role.valueOf(resultSet.getString("role")),
            resultSet.getObject("store_id", Long.class)
    );

    public JdbcMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("member")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Member save(Member member) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("login_id", member.getLoginId())
                .addValue("name", member.getName())
                .addValue("password", member.getPassword())
                .addValue("role", member.getRole().name())
                .addValue("store_id", member.getStoreId());
        Long id = simpleJdbcInsert.executeAndReturnKey(parameters).longValue();
        return Member.restore(id, member.getLoginId(), member.getName(), member.getPassword(), member.getRole(),
                member.getStoreId());
    }

    @Override
    public Optional<Member> findById(Long id) {
        String query = "select * from member where id = ?";
        return jdbcTemplate.query(query, rowMapper, id).stream().findFirst();
    }

    @Override
    public Optional<Member> findByLoginId(String loginId) {
        String query = "select * from member where login_id = ?";
        return jdbcTemplate.query(query, rowMapper, loginId).stream().findFirst();
    }
}
