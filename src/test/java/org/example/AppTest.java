package org.example;

import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
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
    private static int id;
    private static String token;
    private static Connection connection;

    @BeforeEach
    public void setConnect() throws SQLException, IOException {
        FileInputStream fis;
        Properties property = new Properties();
        fis = new FileInputStream("src/test/resources/config.properties");
        property.load(fis);
        String URL_DB = property.getProperty("db.URL_DB");
        String USER_DB = property.getProperty("db.USER_DB");
        String PASSWORD_DB = property.getProperty("db.PASSWORD_DB");
        connection = DriverManager.getConnection(URL_DB, USER_DB, PASSWORD_DB);
    }

    @AfterEach
    public void closeConnect() throws SQLException {
        connection.close();
    }

    @BeforeEach
    public void init() throws IOException {
        FileInputStream fis;
        Properties property = new Properties();
        fis = new FileInputStream("src/test/resources/config.properties");
        property.load(fis);
        String USER_AUTHORIZATION = property.getProperty("db.USER_AUTHORIZATION");
        String PASSWORD_AUTHORIZATION = property.getProperty("db.PASSWORD_AUTHORIZATION");
        String BASE_URI = property.getProperty("db.BASE_URI");
        int PORT = Integer.parseInt(property.getProperty("db.PORT"));
        String BASE_PATH = property.getProperty("db.BASE_PATH");
        authorization = given()
                .baseUri(BASE_URI)
                .port(PORT)
                .basePath(BASE_PATH)
                .contentType(ContentType.JSON)
                .when()
                .body("{ \"password\":\"" + PASSWORD_AUTHORIZATION + "\", " +
                        "\"rememberMe\": true, \"username\":\"" + USER_AUTHORIZATION + "\"}")
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
            id = request
                    .when()
                    .body("{" + "\"regionName\":\"" + TestConst.REGION_NAME + "\"}")
                    .post("/regions/").jsonPath().getInt("id");
            request
                    .when()
                    .body("{" + "\"id\":" + id + "," + "\"regionName\":\"" + TestConst.REGION_NAME_FOR_PATCH + "\"}")
                    .patch("/regions/{id}", id)
                    .then()
                    .statusCode(SC_OK);
            requestForSecondGet
                    .when()
                    .get("/regions/{id}", id)
                    .then()
                    .body("id", is(id),
                            "regionName", is(TestConst.REGION_NAME_FOR_PATCH))
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
                    .body("{" + "\"regionName\":\"" + TestConst.REGION_NAME + "\"}")
                    .post("/regions/").jsonPath().getInt("id");

            request
                    .when()
                    .get("/regions/{id}", id)
                    .then()
                    .body("regionName", is(TestConst.REGION_NAME))
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
                    .body("{" + "\"regionName\":\"" + TestConst.REGION_NAME + "\"}")
                    .post("/regions/");
            id = response.jsonPath().getInt("id");
            request
                    .when()
                    .get("/regions/{id}", id)
                    .then()
                    .body("regionName", is(TestConst.REGION_NAME))
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
        int newRegionId = 0;
        try (
                final PreparedStatement newRegion = connection.prepareStatement(TestConst.SQL_INSERT_INTO_FOR_GET_REGIONS, Statement.RETURN_GENERATED_KEYS)) {
            newRegion.setInt(1, 6666);
            newRegion.setString(2, TestConst.REGION_NAME_ANY);

            assumeTrue(newRegion.executeUpdate() == 1);

            try (final ResultSet generatedKeys = newRegion.getGeneratedKeys()) {
                assumeTrue(generatedKeys.next());
                newRegionId = generatedKeys.getInt(1);
            }
        } catch(SQLException se){
            se.printStackTrace();
        }

        int countRegion = 0;
        try (
                final PreparedStatement countRegions = connection.prepareStatement(TestConst.SQL_SELECT_COUNT_FOR_GET_REGIONS);
                final ResultSet resultSet = countRegions.executeQuery()) {
            assumeTrue(resultSet.next());
            countRegion = resultSet.getInt(1);
        } catch(SQLException se){
            se.printStackTrace();
        }

        try {
            request
                    .when()
                    .get("/regions")
                    .then()
                    .statusCode(SC_OK)
                    .body("size()", is(countRegion), "id", hasItem(newRegionId));
        } finally {
            try (final PreparedStatement deleteRegion = connection.prepareStatement(TestConst.SQL_DELETE_FOR_GET_REGIONS)) {
                deleteRegion.setInt(1, newRegionId);
                assumeTrue(deleteRegion.executeUpdate() == 1);
            }
        }
    }
}

