package roomescape.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
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
class ReservationCancelPromotionE2ETest {

    @LocalServerPort
    int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jdbcTemplate.update("INSERT INTO reservation_time (start_at, finish_at) VALUES ('10:00', '11:00')"); // id=1
        jdbcTemplate.update(
                "INSERT INTO theme (name, description, image_url) VALUES ('테마A', '설명A', 'https://a.com')"); // id=1
    }

    private void insertMember(String loginId, String name) {
        jdbcTemplate.update(
                "INSERT INTO member (login_id, name, password, role) VALUES (?, ?, 'password', 'USER')",
                loginId, name);
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
    @DisplayName("예약자가 취소하면 1번 대기자가 예약으로 승격되고, 2번 대기자의 순번은 1이 된다")
    void 예약_취소_시_대기자_승격_시나리오() {
        insertMember("user1", "user1");
        insertMember("user2", "user2");
        insertMember("user3", "user3");
        Map<String, String> user1Cookies = login("user1", "password");
        Map<String, String> user2Cookies = login("user2", "password");
        Map<String, String> user3Cookies = login("user3", "password");

        Integer reservationId = RestAssured.given().log().all()
                .cookies(user1Cookies)
                .contentType(ContentType.JSON)
                .body(Map.of("date", "2099-12-01", "timeId", 1, "themeId", 1))
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201)
                .extract().path("id");

        RestAssured.given().log().all()
                .cookies(user2Cookies)
                .contentType(ContentType.JSON)
                .body(Map.of("reservationId", reservationId))
                .when().post("/waitings")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .cookies(user3Cookies)
                .contentType(ContentType.JSON)
                .body(Map.of("reservationId", reservationId))
                .when().post("/waitings")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .cookies(user1Cookies)
                .when().delete("/reservations/" + reservationId)
                .then().log().all()
                .statusCode(204);

        List<Map<String, Object>> user1Reservations = RestAssured.given().log().all()
                .cookies(user1Cookies)
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        assertThat(user1Reservations).isEmpty();

        List<Map<String, Object>> user2Reservations = RestAssured.given().log().all()
                .cookies(user2Cookies)
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        assertThat(user2Reservations).hasSize(1);
        assertThat(user2Reservations.get(0).get("name")).isEqualTo("user2");

        List<Map<String, Object>> user2Waitings = RestAssured.given().log().all()
                .cookies(user2Cookies)
                .when().get("/waitings")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        assertThat(user2Waitings).isEmpty();

        List<Map<String, Object>> user3Waitings = RestAssured.given().log().all()
                .cookies(user3Cookies)
                .when().get("/waitings")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        assertThat(user3Waitings).hasSize(1);
        assertThat(user3Waitings.get(0).get("turn")).isEqualTo(1);
    }
}
