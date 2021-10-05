package org.example;

public class TestConst {
    public static final String REGION_NAME = "Тамбов4";
    public static final String REGION_NAME_FOR_PATCH = "Саратов4";
    public static final String REGION_NAME_ANY = "Липецк";

    public static final String SQL_INSERT_INTO_FOR_GET_REGIONS = "INSERT INTO region(id,region_name) VALUES(?,?)";
    public static final String SQL_SELECT_COUNT_FOR_GET_REGIONS = "SELECT COUNT(*) FROM region";
    public static final String SQL_DELETE_FOR_GET_REGIONS = "DELETE FROM region WHERE ID=?";
}
