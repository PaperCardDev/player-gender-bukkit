package cn.paper_card.player_gender;

import cn.paper_card.database.api.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

class Table {
    private static final String NAME = "gender";

    private final @NotNull Connection connection;

    private PreparedStatement statementInsert = null;
    private PreparedStatement statementUpdateGender = null;

    private PreparedStatement statementQueryGender = null;


    Table(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.create();
    }

    private void create() throws SQLException {
        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s (
                    uid1    BIGINT NOT NULL,
                    uid2    BIGINT NOT NULL,
                    gender  INT NOT NULL,
                    PRIMARY KEY(uid1, uid2)
                )""".formatted(NAME));
    }


    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement
                    ("INSERT INTO %s (uid1, uid2, gender) VALUES (?, ?, ?)".formatted(NAME));
        }
        return this.statementInsert;
    }


    private @NotNull PreparedStatement getStatementUpdateGender() throws SQLException {
        if (this.statementUpdateGender == null) {
            this.statementUpdateGender = this.connection.prepareStatement
                    ("UPDATE %s SET gender=? WHERE uid1=? AND uid2=?".formatted(NAME));
        }
        return this.statementUpdateGender;
    }

    private @NotNull PreparedStatement getStatementQueryGender() throws SQLException {
        if (this.statementQueryGender == null) {
            this.statementQueryGender = this.connection.prepareStatement
                    ("SELECT gender FROM %s WHERE uid1=? AND uid2=?".formatted(NAME));
        }
        return this.statementQueryGender;
    }

    int insert(@NotNull UUID uuid, int gender) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        ps.setInt(3, gender);
        return ps.executeUpdate();
    }

    int updateGender(@NotNull UUID uuid, int gender) throws SQLException {
        final PreparedStatement ps = this.getStatementUpdateGender();
        ps.setInt(1, gender);
        ps.setLong(2, uuid.getMostSignificantBits());
        ps.setLong(3, uuid.getLeastSignificantBits());
        return ps.executeUpdate();
    }

    @Nullable Integer queryGender(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryGender();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());

        final ResultSet resultSet = ps.executeQuery();

        final Integer gender;

        try {
            if (resultSet.next()) {
                gender = resultSet.getInt(1);
            } else gender = null;
            if (resultSet.next()) throw new SQLException("不应该还有数据！");
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        return gender;
    }
}
