package roomescape.e2e;

import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = {"/truncate.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class MissionStep1Test {

    @LocalServerPort
    int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, String> testerCookies;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('10:00', '11:00')");
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('14:00', '15:00')");
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('18:00', '19:00')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마A', '설명A', 'https://a.com')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마B', '설명B', 'https://b.com')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마C', '설명C', 'https://c.com')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마D', '설명D', 'https://d.com')");

        Long u1Id = insertMember("u1", "u1");
        Long u2Id = insertMember("u2", "u2");
        Long u3Id = insertMember("u3", "u3");

        jdbcTemplate.update("INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, 1, 1)",
                u1Id, LocalDate.now().minusDays(1));
        jdbcTemplate.update("INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, 1, 1)",
                u2Id, LocalDate.now().minusDays(2));
        jdbcTemplate.update("INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, 1, 1)",
                u3Id, LocalDate.now().minusDays(3));
        jdbcTemplate.update("INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, 2, 2)",
                u1Id, LocalDate.now().minusDays(1));
        jdbcTemplate.update("INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, 2, 2)",
                u2Id, LocalDate.now().minusDays(2));
        jdbcTemplate.update("INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, 3, 3)",
                u1Id, LocalDate.now().minusDays(1));

        insertMember("tester", "테스터");
        testerCookies = login("tester", "password");
    }

    private Long insertMember(String loginId, String name) {
        jdbcTemplate.update(
                "INSERT INTO member (login_id, name, password, role) VALUES (?, ?, 'password', 'USER')",
                loginId, name);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM member", Long.class);
    }

    private Map<String, String> login(String loginId, String password) {
        String sessionId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("loginId", loginId, "password", password))
                .when().post("/login")
                .then().extract().cookie("JSESSIONID");
        return Map.of("JSESSIONID", sessionId);
    }

    @Test
    void 예약_가능_시간_정상_흐름() {
        LocalDate date = LocalDate.now().plusDays(11);
        RestAssured.given().log().all()
                .when().get("/times/available?date=" + date.toString() + "&themeId=1")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(3));

        RestAssured.given().log().all()
                .cookies(testerCookies)
                .contentType(ContentType.JSON)
                .body(Map.of("date", date.toString(), "timeId", 1, "themeId", 1))
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .when().get("/times/available?date=" + date.toString() + "&themeId=1")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    void 인기_테마_조회() {
        RestAssured.given().log().all()
                .when().get("/themes/top/3")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(3))
                .body("[0].name", is("테마A"))
                .body("[1].name", is("테마B"))
                .body("[2].name", is("테마C"));
    }
}
