package com.nabta.backend.academic;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.UUID;

/**
 * Academic demonstration of INSERT, SELECT, UPDATE and DELETE with PreparedStatement.
 * Uses a temporary product row inside a transaction and rolls back on failure.
 * Run only against the local/test Nabta database.
 */
public class JdbcPreparedStatementDemo {
    private static final String DB_URL = System.getenv().getOrDefault(
            "NABTA_DB_URL",
            "jdbc:mysql://localhost:3306/nabta_db?useSSL=false&serverTimezone=UTC");

    public static void main(String[] args) throws Exception {
        String username = System.getenv("NABTA_DB_USERNAME");
        String password = System.getenv("NABTA_DB_PASSWORD");
        if (username == null || username.isBlank() || password == null) {
            throw new IllegalStateException(
                    "Set NABTA_DB_USERNAME and NABTA_DB_PASSWORD in this PowerShell session first.");
        }

        try (Connection connection = DriverManager.getConnection(DB_URL, username, password)) {
            connection.setAutoCommit(false);
            try {
                long categoryId = findCategoryId(connection);
                String testName = "JDBC-DEMO-" + UUID.randomUUID().toString().substring(0, 8);
                long productId = insertTestProduct(connection, categoryId, testName);
                System.out.println("INSERT: created temporary test product id=" + productId);

                printProduct(connection, productId, "SELECT after INSERT");

                String updatedName = testName + "-UPDATED";
                updateTestProduct(connection, productId, updatedName);
                System.out.println("UPDATE: changed temporary product name and price.");
                printProduct(connection, productId, "SELECT after UPDATE");

                deleteTestProduct(connection, productId);
                System.out.println("DELETE: temporary product deleted.");
                System.out.println("All four PreparedStatement CRUD steps completed successfully.");
                connection.rollback(); // Guarantees this demo does not leave test data behind.
                System.out.println("ROLLBACK: transaction reverted; no demo product remains.");
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private static long findCategoryId(Connection connection) throws SQLException {
        String sql = "SELECT id FROM categories ORDER BY id LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("No category exists in nabta_db; cannot create test product.");
            }
            return rs.getLong("id");
        }
    }

    private static long insertTestProduct(Connection connection, long categoryId, String testName)
            throws SQLException {
        String sql = "INSERT INTO products "
                + "(is_active, created_at, description, image_url, name, price, stock_quantity, "
                + "updated_at, category_id, description_ar, name_ar) "
                + "VALUES (?, CURRENT_TIMESTAMP(6), ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBoolean(1, true);
            ps.setString(2, "Temporary JDBC PreparedStatement demonstration product");
            ps.setNull(3, Types.VARCHAR);
            ps.setString(4, testName);
            ps.setBigDecimal(5, new BigDecimal("1.00"));
            ps.setInt(6, 1);
            ps.setLong(7, categoryId);
            ps.setString(8, "اختبار JDBC");
            ps.setString(9, "منتج تجريبي");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL did not return a generated product ID.");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void printProduct(Connection connection, long productId, String label)
            throws SQLException {
        String sql = "SELECT id, name, name_ar, price, stock_quantity FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Test product not found during " + label + ".");
                }
                System.out.printf("%s: id=%d, name=%s, name_ar=%s, price=%s, stock=%d%n",
                        label, rs.getLong("id"), rs.getString("name"),
                        rs.getString("name_ar"), rs.getBigDecimal("price"),
                        rs.getInt("stock_quantity"));
            }
        }
    }

    private static void updateTestProduct(Connection connection, long productId, String updatedName)
            throws SQLException {
        String sql = "UPDATE products SET name = ?, name_ar = ?, price = ?, "
                + "updated_at = CURRENT_TIMESTAMP(6) WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, updatedName);
            ps.setString(2, "تم تحديث اختبار JDBC");
            ps.setBigDecimal(3, new BigDecimal("2.00"));
            ps.setLong(4, productId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Expected one test product to be updated.");
            }
        }
    }

    private static void deleteTestProduct(Connection connection, long productId) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, productId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Expected one test product to be deleted.");
            }
        }
    }
}
