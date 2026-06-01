package com.back.simpleDb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Sql {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final SimpleDb simpleDb;
    private final StringBuilder rawSqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... params) {
        if (!rawSqlBuilder.isEmpty()) {
            rawSqlBuilder.append("\n");
        }

        rawSqlBuilder.append(sqlPart);

        for (Object param : params) {
            this.params.add(param);
        }

        return this;
    }

    public Sql appendIn(String sqlPart, Object... values) {
        List<Object> flattenedValues = flatten(values);
        String placeholders = String.join(", ", Collections.nCopies(flattenedValues.size(), "?"));

        if (flattenedValues.isEmpty()) {
            placeholders = "NULL";
        }

        String expandedSqlPart = sqlPart.replaceFirst("\\?", placeholders);
        append(expandedSqlPart);
        params.addAll(flattenedValues);

        return this;
    }

    public long insert() {
        String rawSql = getRawSql();
        logIfDevMode(rawSql);

        try (PreparedStatement statement = simpleDb.prepareStatement(rawSql, true)) {
            simpleDb.bindParams(statement, params.toArray());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed: " + rawSql, e);
        }
    }

    public int update() {
        return executeUpdate();
    }

    public int delete() {
        return executeUpdate();
    }

    public List<Map<String, Object>> selectRows() {
        String rawSql = getRawSql();
        logIfDevMode(rawSql);

        try (PreparedStatement statement = simpleDb.prepareStatement(rawSql, false)) {
            simpleDb.bindParams(statement, params.toArray());

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();

                while (resultSet.next()) {
                    rows.add(toRow(resultSet));
                }

                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Select failed: " + rawSql, e);
        }
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();

        if (rows.isEmpty()) {
            return null;
        }

        return rows.getFirst();
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        return selectRows().stream()
                .map(row -> objectMapper.convertValue(row, clazz))
                .toList();
    }

    public <T> T selectRow(Class<T> clazz) {
        Map<String, Object> row = selectRow();

        if (row == null) {
            return null;
        }

        return objectMapper.convertValue(row, clazz);
    }

    public Long selectLong() {
        Object value = selectScalar();

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    public List<Long> selectLongs() {
        return selectRows().stream()
                .map(row -> row.values().iterator().next())
                .map(value -> {
                    if (value instanceof Number number) {
                        return number.longValue();
                    }

                    return Long.parseLong(value.toString());
                })
                .toList();
    }

    public String selectString() {
        Object value = selectScalar();

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    public Boolean selectBoolean() {
        Object value = selectScalar();

        return toBoolean(value);
    }

    public LocalDateTime selectDatetime() {
        Object value = selectScalar();

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        if (value == null) {
            return null;
        }

        return LocalDateTime.parse(value.toString());
    }

    private int executeUpdate() {
        String rawSql = getRawSql();
        logIfDevMode(rawSql);

        try (PreparedStatement statement = simpleDb.prepareStatement(rawSql, false)) {
            simpleDb.bindParams(statement, params.toArray());

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + rawSql, e);
        }
    }

    private Object selectScalar() {
        Map<String, Object> row = selectRow();

        if (row == null || row.isEmpty()) {
            return null;
        }

        return row.values().iterator().next();
    }

    private Map<String, Object> toRow(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();

        for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
            String columnName = metaData.getColumnLabel(columnIndex);
            Object value = normalizeValue(resultSet.getObject(columnIndex), metaData.getColumnType(columnIndex));

            row.put(columnName, value);
        }

        return row;
    }

    private Object normalizeValue(Object value, int columnType) {
        if (columnType == Types.BIT || columnType == Types.BOOLEAN) {
            return toBoolean(value);
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        if (value instanceof Byte byteValue) {
            return byteValue.longValue();
        }

        if (value instanceof Short shortValue) {
            return shortValue.longValue();
        }

        if (value instanceof Integer integer) {
            return integer.longValue();
        }

        if (value instanceof byte[] bytes && bytes.length == 1) {
            return bytes[0] != 0;
        }

        return value;
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof Number number) {
            return number.longValue() != 0L;
        }

        if (value instanceof byte[] bytes && bytes.length > 0) {
            return bytes[0] != 0;
        }

        return Boolean.parseBoolean(value.toString());
    }

    private List<Object> flatten(Object[] values) {
        List<Object> flattenedValues = new ArrayList<>();

        for (Object value : values) {
            if (value == null) {
                flattenedValues.add(null);
                continue;
            }

            Class<?> valueClass = value.getClass();

            if (valueClass.isArray()) {
                int length = Array.getLength(value);

                for (int i = 0; i < length; i++) {
                    flattenedValues.add(Array.get(value, i));
                }

                continue;
            }

            flattenedValues.add(value);
        }

        return flattenedValues;
    }

    private String getRawSql() {
        return rawSqlBuilder.toString();
    }

    private void logIfDevMode(String rawSql) {
        if (simpleDb.isDevMode()) {
            simpleDb.log(rawSql, params);
        }
    }
}
