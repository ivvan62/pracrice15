package org.example;

import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class AppTest {
    private static RequestSpecification request;
    private static RequestSpecification requestForDelete;
    private static RequestSpecification requestForSecondGet;
    private static Response authorization;
    private static Response response;
    private static final String BASE_URI = "http://31.131.249.140";
    private static final int PORT = 8080;
    private static final String BASE_PATH = "/api/";
    private static int id;
    private static String token;
    private static Connection connection;
    private static final String URL = "jdbc:postgresql://31.131.249.140:5432/app-db";
    private static final String USER = "app-db-admin";
    private static final String PASSWORD = "AiIoqv6c2k0gVhx2";
    private static final String regionName = "Тамбов4";
    private static final String regionNameForPatch = "Саратов4";

    @BeforeEach
    public void setConnect() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    @AfterEach
    public void closeConnect() throws SQLException {
        connection.close();
    }

    @BeforeEach
    public void init() {
        authorization = given()
                .baseUri(BASE_URI)
                .port(PORT)
                .basePath(BASE_PATH)
                .contentType(ContentType.JSON)
                .when()
                .body("{ \"password\": \"kMBc3Lb7iM3sd0Mt\", \"rememberMe\": true, \"username\": \"user\" }")
                .post("/authenticate");
        token = authorization.jsonPath().getString("id_token");
        request = given()
                .baseUri(BASE_URI)
                .port(PORT)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .filters(new RequestLoggingFilter(), new RequestLoggingFilter());
        requestForDelete = given()
                .baseUri(BASE_URI)
                .port(PORT)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .filters(new RequestLoggingFilter(), new RequestLoggingFilter());
        requestForSecondGet = given()
                .baseUri(BASE_URI)
                .port(PORT)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .filters(new RequestLoggingFilter(), new RequestLoggingFilter());
    }

    @Test
    @DisplayName("Проверяем PATCH /api/regions/{id}")
    public void shouldPatchRegionById() {
        try {
            response = request
                    .when()
                    .body("{" + "\"regionName\":\"" + regionName + "\"}")
                    .post("/regions/");
            request
                    .when()
                    .body("{" + "\"regionName\":\"" + regionName + "\"}")
                    .post("/regions/");

            id = response.jsonPath().getInt("id");
            request
                    .when()
                    .body("{" + "\"id\":" + id + "," + "\"regionName\":\"" + regionNameForPatch + "\"}")
                    .patch("/regions/{id}", id)
                    .then()
                    .statusCode(SC_OK);
            requestForSecondGet
                    .when()
                    .get("/regions/{id}", id)
                    .then()
                    .body("id", is(id),
                            "regionName", is(regionNameForPatch))
                    .statusCode(SC_OK);
        } finally {
            requestForDelete
                    .when()
                    .delete("/regions/{id}", id)
                    .then()
                    .statusCode(SC_NO_CONTENT);
        }
    }

    @Test
    @DisplayName("Проверяем GET /api/regions/{id}")
    public void shouldGetRegionById() {
        try {
            id = request
                    .when()
                    .body("{" + "\"regionName\":\"" + regionName + "\"}")
                    .post("/regions/").jsonPath().getInt("id");

            request
                    .when()
                    .get("/regions/{id}", id)
                    .then()
                    .body("regionName", is(regionName))
                    .statusCode(SC_OK);
        } finally {
            requestForDelete
                    .when()
                    .delete("/regions/{id}", id)
                    .then()
                    .statusCode(SC_NO_CONTENT);
        }

    }

    @Test
    @DisplayName("Проверяем DEL /api/regions/{id}")
    public void shouldDelRegionById() {
        try {
            response = request
                    .when()
                    .body("{" + "\"regionName\":\"" + regionName + "\"}")
                    .post("/regions/");
            id = response.jsonPath().getInt("id");
            request
                    .when()
                    .get("/regions/{id}", id)
                    .then()
                    .body("regionName", is(regionName))
                    .statusCode(SC_OK);
        } finally {
            requestForDelete
                    .when()
                    .delete("/regions/{id}", id)
                    .then()
                    .statusCode(SC_NO_CONTENT);
            requestForSecondGet
                    .when()
                    .get("/regions/{id}", id)
                    .then()
                    .statusCode(SC_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("Проверяем GET /api/account")
    public void shouldGetAccounts() {
        request
                .when()
                .get("/account/")
                .then()
                .statusCode(200)
                .body("id", is(2),
                        "login", is("user"),
                        "firstName", is("User"),
                        "lastName", is("User"),
                        "email", is("user@localhost"),
                        "imageUrl", is(""),
                        "activated", is(true),
                        "langKey", is("en"),
                        "createdBy", is("system"),
                        "lastModifiedBy", is("user"));
    }

    @Test
    @DisplayName("Проверяем отсуствие страны по айди GET /api/countries/{id}")
    public void shouldGetNotFoundCountries() {
        request
                .when()
                .get("/countries/{id}", 2)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Проверяем GET /api/regions")
    public void shouldGetRegions() throws SQLException {
        int newRegionId;
        try (
                final PreparedStatement newRegion = connection.prepareStatement("INSERT INTO region(id,region_name) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            newRegion.setInt(1, 6666);
            newRegion.setString(2, "Липецк");

            assumeTrue(newRegion.executeUpdate() == 1);

            try (final ResultSet generatedKeys = newRegion.getGeneratedKeys()) {
                assumeTrue(generatedKeys.next());
                newRegionId = generatedKeys.getInt(1);
            }
        }

        int countRegion;
        try (
                final PreparedStatement countRegions = connection.prepareStatement("SELECT COUNT(*) FROM region");
                final ResultSet resultSet = countRegions.executeQuery()) {
            assumeTrue(resultSet.next());
            countRegion = resultSet.getInt(1);
        }

        try {
            request
                    .when()
                    .get("/regions")
                    .then()
                    .statusCode(SC_OK)
                    .body("size()", is(countRegion), "id", hasItem(newRegionId));
        } finally {
            try (final PreparedStatement deleteRegion = connection.prepareStatement("DELETE FROM region WHERE ID=?")) {
                deleteRegion.setInt(1, newRegionId);
                assumeTrue(deleteRegion.executeUpdate() == 1);
            }
        }
    }
}

