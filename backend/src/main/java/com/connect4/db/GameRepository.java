package com.connect4.db;

import com.connect4.game.Connect4Engine;
import com.connect4.game.GameStatus;
import com.connect4.game.MoveResult;
import com.connect4.game.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class GameRepository {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public GameRepository(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public long createGame() throws SQLException {
        String sql = "INSERT INTO games (status, current_player, winner, board_state) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, GameStatus.IN_PROGRESS.name());
            stmt.setString(2, Player.P1.name());
            stmt.setNull(3, Types.VARCHAR);
            stmt.setString(4, Connect4Engine.defaultBoardString());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Failed to retrieve generated game ID.");
            }
        }
    }

    public Connect4Engine loadGame(long gameId) throws SQLException {
        String sql = "SELECT status, current_player, winner, board_state FROM games WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, gameId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Game not found: " + gameId);
                }
                GameStatus status = GameStatus.valueOf(rs.getString("status"));
                Player currentPlayer = Player.valueOf(rs.getString("current_player"));
                String winnerStr = rs.getString("winner");
                Player winner = winnerStr != null ? Player.valueOf(winnerStr) : null;
                String boardState = rs.getString("board_state");

                return new Connect4Engine(boardState, currentPlayer, status, winner);
            }
        }
    }

    public MoveResult applyMove(long gameId, Player player, int col) throws SQLException {
        String selectGameSql = "SELECT status, current_player, winner, board_state FROM games WHERE id = ? FOR UPDATE";
        String nextMoveNumSql = "SELECT COALESCE(MAX(move_number), 0) + 1 FROM moves WHERE game_id = ?";
        String insertMoveSql = "INSERT INTO moves (game_id, move_number, player, column_idx, row_idx) VALUES (?, ?, ?, ?, ?)";
        String updateGameSql = "UPDATE games SET status = ?, current_player = ?, winner = ?, board_state = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                Connect4Engine engine;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectGameSql)) {
                    selectStmt.setLong(1, gameId);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Game not found: " + gameId);
                        }
                        GameStatus status = GameStatus.valueOf(rs.getString("status"));
                        Player currentPlayer = Player.valueOf(rs.getString("current_player"));
                        String winnerStr = rs.getString("winner");
                        Player winner = winnerStr != null ? Player.valueOf(winnerStr) : null;
                        String boardState = rs.getString("board_state");
                        engine = new Connect4Engine(boardState, currentPlayer, status, winner);
                    }
                }

                MoveResult result = engine.makeMove(player, col);

                int moveNumber;
                try (PreparedStatement moveNumStmt = conn.prepareStatement(nextMoveNumSql)) {
                    moveNumStmt.setLong(1, gameId);
                    try (ResultSet rs = moveNumStmt.executeQuery()) {
                        rs.next();
                        moveNumber = rs.getInt(1);
                    }
                }

                try (PreparedStatement insertMoveStmt = conn.prepareStatement(insertMoveSql)) {
                    insertMoveStmt.setLong(1, gameId);
                    insertMoveStmt.setInt(2, moveNumber);
                    insertMoveStmt.setString(3, result.movedBy().name());
                    insertMoveStmt.setInt(4, result.col());
                    insertMoveStmt.setInt(5, result.row());
                    insertMoveStmt.executeUpdate();
                }

                try (PreparedStatement updateGameStmt = conn.prepareStatement(updateGameSql)) {
                    updateGameStmt.setString(1, result.status().name());
                    Player persistedNext = result.nextPlayer() != null ? result.nextPlayer() : engine.getCurrentPlayer();
                    updateGameStmt.setString(2, persistedNext.name());
                    if (result.winner() != null) {
                        updateGameStmt.setString(3, result.winner().name());
                    } else {
                        updateGameStmt.setNull(3, Types.VARCHAR);
                    }
                    updateGameStmt.setString(4, result.boardString());
                    updateGameStmt.setLong(5, gameId);
                    updateGameStmt.executeUpdate();
                }

                conn.commit();
                return result;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof SQLException sqlEx) throw sqlEx;
                if (ex instanceof RuntimeException rtEx) throw rtEx;
                throw new SQLException("Failed to apply move", ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}