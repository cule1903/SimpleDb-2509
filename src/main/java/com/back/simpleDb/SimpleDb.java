package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public class SimpleDb {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();
    private volatile boolean devMode;

    public SimpleDb(String host, String username, String password, String dbName) {
        this.jdbcUrl = "jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul&characterEncoding=utf8".formatted(host, dbName);
        this.username = username;
        this.password = password;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    boolean isDevMode() {
        return devMode;
    }

    Connection getConnection() {
        try {
            Connection connection = threadConnection.get();

            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
                threadConnection.set(connection);
            }

            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("DB connection failed.", e);
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public int run(String rawSql, Object... params) {
        if (devMode) {
            log(rawSql, Arrays.asList(params));
        }

        try (PreparedStatement statement = getConnection().prepareStatement(rawSql)) {
            bindParams(statement, params);

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("SQL execution failed: " + rawSql, e);
        }
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException("Transaction start failed.", e);
        }
    }

    public void commit() {
        try {
            Connection connection = getConnection();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Transaction commit failed.", e);
        }
    }

    public void rollback() {
        try {
            Connection connection = getConnection();
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Transaction rollback failed.", e);
        }
    }

    public void close() {
        Connection connection = threadConnection.get();

        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB connection close failed.", e);
        } finally {
            threadConnection.remove();
        }
    }

    void bindParams(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    PreparedStatement prepareStatement(String rawSql, boolean returnGeneratedKeys) throws SQLException {
        if (returnGeneratedKeys) {
            return getConnection().prepareStatement(rawSql, Statement.RETURN_GENERATED_KEYS);
        }

        return getConnection().prepareStatement(rawSql);
    }

    void log(String rawSql, Iterable<?> params) {
        System.out.println("== rawSql ==");
        System.out.println(rawSql);
        System.out.println("== params ==");
        System.out.println(params);
    }
}
