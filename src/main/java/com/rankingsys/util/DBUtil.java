package com.rankingsys.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBUtil {
    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/ranking_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            //here is the private key for you to connect the database
    private static final String JDBC_USER = "root";

    private static final String JDBC_PASSWORD = "12345678";



    private DBUtil() {
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }
}
