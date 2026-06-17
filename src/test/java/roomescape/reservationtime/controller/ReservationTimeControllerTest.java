package roomescape.reservationtime.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
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
class ReservationTimeControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, String> adminCookies;
    private Map<String, String> userCookies;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('10:00', '11:00')");
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('14:00', '15:00')");
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('18:00', '19:00')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마A', '설명A', 'https://a.com')");
        jdbcTemplate.update("INSERT INTO member (login_id, name, password, role) VALUES ('user1', '현미밥', 'password', 'USER')");
        Long memberId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM member", Long.class);
        jdbcTemplate.update("INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, 1, 1)",
                memberId, LocalDate.now().minusDays(1));
        jdbcTemplate.update(
                "INSERT INTO member (login_id, name, password, role) VALUES ('admin', '관리자', 'password', 'ADMIN')");
        adminCookies = login("admin", "password");
        userCookies = login("user1", "password");
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
    @DisplayName("시간 생성 성공")
    void 시간_생성_성공() {
        RestAssured.given().log().all()
                .cookies(adminCookies)
                .contentType(ContentType.JSON)
                .body(Map.of("startAt", "20:00", "finishAt", "21:00"))
                .when().post("/times")
                .then().log().all()
                .statusCode(201)
                .body("startAt", equalTo("20:00:00"));
    }

    @Test
    @DisplayName("시간 전체 조회 성공")
    void 시간_전체_조회_성공() {
        RestAssured.given().log().all()
                .when().get("/times")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(3));
    }

    @Test
    @DisplayName("시간 삭제 성공")
    void 시간_삭제_성공() {
        Integer id = RestAssured.given().log().all()
                .cookies(adminCookies)
                .contentType(ContentType.JSON)
                .body(Map.of("startAt", "20:00", "finishAt", "21:00"))
                .when().post("/times")
                .then().extract().path("id");

        RestAssured.given().log().all()
                .cookies(adminCookies)
                .when().delete("/times/" + id)
                .then().log().all()
                .statusCode(204);
    }

    @Test
    @DisplayName("예약 가능 시간 조회 성공")
    void 예약_가능_시간_조회_성공() {
        String date = LocalDate.now().minusDays(1).toString();
        RestAssured.given().log().all()
                .when().get("/times/available?date=" + date + "&themeId=1")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @DisplayName("일반 사용자가 시간 생성 시 403")
    void 일반_사용자_시간_생성_실패() {
        RestAssured.given().log().all()
                .cookies(userCookies)
                .contentType(ContentType.JSON)
                .body(Map.of("startAt", "20:00", "finishAt", "21:00"))
                .when().post("/times")
                .then().log().all()
                .statusCode(403)
                .body("errorCode", equalTo("ADMIN_ACCESS_REQUIRED"));
    }

    @Test
    @DisplayName("일반 사용자가 시간 삭제 시 403")
    void 일반_사용자_시간_삭제_실패() {
        RestAssured.given().log().all()
                .cookies(userCookies)
                .when().delete("/times/2")
                .then().log().all()
                .statusCode(403)
                .body("errorCode", equalTo("ADMIN_ACCESS_REQUIRED"));
    }
}
