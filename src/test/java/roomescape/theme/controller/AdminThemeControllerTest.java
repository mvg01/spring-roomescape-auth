package roomescape.theme.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
class AdminThemeControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, String> adminCookies;
    private Map<String, String> userCookies;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마A', '설명A', 'https://a.com')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마B', '설명B', 'https://b.com')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마C', '설명C', 'https://c.com')");
        jdbcTemplate.update("INSERT INTO theme (name, description, image_url) VALUES ('테마D', '설명D', 'https://d.com')");
        jdbcTemplate.update(
                "INSERT INTO member (login_id, name, password, role) VALUES ('admin', '관리자', 'password', 'ADMIN')");
        jdbcTemplate.update(
                "INSERT INTO member (login_id, name, password, role) VALUES ('user1', '현미밥', 'password', 'USER')");
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

    private Map<String, String> themeBody() {
        return Map.of("name", "테마5", "description", "설명", "imageUrl", "https://image.com");
    }

    @Test
    @DisplayName("테마 생성 성공")
    void 테마_생성_성공() {
        RestAssured.given().log().all()
                .cookies(adminCookies)
                .contentType(ContentType.JSON)
                .body(themeBody())
                .when().post("/admin/themes")
                .then().log().all()
                .statusCode(201)
                .body("name", equalTo("테마5"));
    }

    @Test
    @DisplayName("테마 전체 조회 성공")
    void 테마_전체_조회_성공() {
        RestAssured.given().log().all()
                .cookies(adminCookies)
                .when().get("/admin/themes")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(4));
    }

    @Test
    @DisplayName("테마 삭제 성공")
    void 테마_삭제_성공() {
        Integer id = RestAssured.given().log().all()
                .cookies(adminCookies)
                .contentType(ContentType.JSON)
                .body(themeBody())
                .when().post("/admin/themes")
                .then().extract().path("id");

        RestAssured.given().log().all()
                .cookies(adminCookies)
                .when().delete("/admin/themes/" + id)
                .then().log().all()
                .statusCode(204);
    }

    @Test
    @DisplayName("일반 사용자가 테마 생성 시 403")
    void 일반_사용자_테마_생성_실패() {
        RestAssured.given().log().all()
                .cookies(userCookies)
                .contentType(ContentType.JSON)
                .body(themeBody())
                .when().post("/admin/themes")
                .then().log().all()
                .statusCode(403)
                .body("errorCode", equalTo("ADMIN_ACCESS_REQUIRED"));
    }

    @Test
    @DisplayName("일반 사용자가 테마 삭제 시 403")
    void 일반_사용자_테마_삭제_실패() {
        RestAssured.given().log().all()
                .cookies(userCookies)
                .when().delete("/admin/themes/1")
                .then().log().all()
                .statusCode(403)
                .body("errorCode", equalTo("ADMIN_ACCESS_REQUIRED"));
    }
}
