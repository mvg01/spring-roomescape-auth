package roomescape.e2e;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = {"/truncate.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class Roomescapee2eTest {

    @LocalServerPort
    int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long reservationId;
    private Integer existingWaiting1Id;
    private Map<String, String> user2Cookies;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('10:00', '11:00')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마A', '설명A', 'https://a.com')");

        Long user1Id = insertMember("user1", "user1");
        insertMember("user2", "user2");

        jdbcTemplate.update(
                "INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, '2099-12-01', 1, 1)",
                user1Id);
        reservationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservation", Long.class);

        user2Cookies = login("user2", "password");

        existingWaiting1Id = RestAssured.given().contentType(ContentType.JSON)
                .cookies(user2Cookies)
                .body(Map.of("reservationId", reservationId))
                .post("/waitings").then().extract().path("id");
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
    @DisplayName("대기 생성 -> 조회 -> 순번 확인")
    void 대기_순번_확인_테스트() {
        insertMember("hyunmibob", "현미밥");
        Map<String, String> hyunmibobCookies = login("hyunmibob", "password");
        insertMember("hyunmibob3", "현미밥3");
        Map<String, String> hyunmibob3Cookies = login("hyunmibob3", "password");

        RestAssured.given().log().all()
                .cookies(hyunmibobCookies)
                .contentType(ContentType.JSON)
                .body(Map.of("reservationId", reservationId))
                .when().post("/waitings")
                .then().log().all()
                .statusCode(201)
                .body("name", equalTo("현미밥"));

        Integer waiting3Id = RestAssured.given().log().all()
                .cookies(hyunmibob3Cookies)
                .contentType(ContentType.JSON)
                .body(Map.of("reservationId", reservationId))
                .when().post("/waitings")
                .then().log().all()
                .statusCode(201)
                .extract().path("id");

        // 현미밥3은 3번째로 등록 → turn=3
        RestAssured.given().log().all()
                .cookies(hyunmibob3Cookies)
                .when().get("/waitings")
                .then().log().all()
                .statusCode(200)
                .body("[0].id", equalTo(waiting3Id))
                .body("[0].turn", equalTo(3));

        // user2는 1번째로 등록 → turn=1
        RestAssured.given().log().all()
                .cookies(user2Cookies)
                .when().get("/waitings")
                .then().log().all()
                .statusCode(200)
                .body("[0].id", equalTo(existingWaiting1Id))
                .body("[0].turn", equalTo(1));
    }

    @Test
    @DisplayName("대기 취소 후 순번 재정렬")
    void 순번_정렬_테스트() {
        insertMember("hyunmibob", "현미밥");
        Map<String, String> hyunmibobCookies = login("hyunmibob", "password");
        insertMember("hyunmibob3", "현미밥3");
        Map<String, String> hyunmibob3Cookies = login("hyunmibob3", "password");

        RestAssured.given().log().all()
                .cookies(hyunmibobCookies)
                .contentType(ContentType.JSON)
                .body(Map.of("reservationId", reservationId))
                .when().post("/waitings")
                .then().log().all()
                .statusCode(201);

        Integer waiting3Id = RestAssured.given().log().all()
                .cookies(hyunmibob3Cookies)
                .contentType(ContentType.JSON)
                .body(Map.of("reservationId", reservationId))
                .when().post("/waitings")
                .then().log().all()
                .statusCode(201)
                .extract().path("id");

        // turn=1인 user2 취소
        RestAssured.given().log().all()
                .cookies(user2Cookies)
                .when().delete("/waitings/" + existingWaiting1Id)
                .then().log().all()
                .statusCode(204);

        // 현미밥3의 순번이 3→2로 당겨짐
        RestAssured.given().log().all()
                .cookies(hyunmibob3Cookies)
                .when().get("/waitings")
                .then().log().all()
                .statusCode(200)
                .body("[0].id", equalTo(waiting3Id))
                .body("[0].turn", equalTo(2));
    }
}
