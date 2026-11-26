package com.connect4.game;

public class Connect4Engine {
    public static final int ROWS = 6;
    public static final int COLS = 7;
    public static final int WIN_LENGTH = 4;
    public static final char EMPTY = '.';

    private static final int[][] DIRECTIONS = {
        {0, 1},   // Horizontal
        {1, 0},   // Vertical
        {1, 1},   // Diagonal down-right
        {1, -1}   // Diagonal down-left
    };

    private final char[][] grid = new char[ROWS][COLS];
    private Player currentPlayer;
    private GameStatus status;
    private Player winner;

    public Connect4Engine() {
        this(defaultBoardString(), Player.P1, GameStatus.IN_PROGRESS, null);
    }

    public Connect4Engine(String boardString, Player currentPlayer, GameStatus status, Player winner) {
        loadBoardString(boardString);
        this.currentPlayer = currentPlayer;
        this.status = status;
        this.winner = winner;
    }

    public static String defaultBoardString() {
        return String.valueOf(EMPTY).repeat(ROWS * COLS);
    }

    public void loadBoardString(String boardString) {
        if (boardString == null || boardString.length() != ROWS * COLS) {
            throw new IllegalArgumentException("Board string must be exactly " + (ROWS * COLS) + " characters.");
        }
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                char ch = boardString.charAt(r * COLS + c);
                if (ch != EMPTY && ch != Player.P1.symbol() && ch != Player.P2.symbol()) {
                    throw new IllegalArgumentException("Invalid board character at index " + (r * COLS + c) + ": " + ch);
                }
                grid[r][c] = ch;
            }
        }
    }

    public String toBoardString() {
        StringBuilder sb = new StringBuilder(ROWS * COLS);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sb.append(grid[r][c]);
            }
        }
        return sb.toString();
    }

    public MoveResult makeMove(Player player, int col) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is already finished with status: " + status);
        }
        if (player != currentPlayer) {
            throw new IllegalArgumentException("It is " + currentPlayer + "'s turn, not " + player);
        }
        if (col < 0 || col >= COLS) {
            throw new IllegalArgumentException("Column out of bounds (0..6): " + col);
        }

        int targetRow = findDropRow(col);
        if (targetRow == -1) {
            throw new IllegalStateException("Column " + col + " is full.");
        }

        grid[targetRow][col] = player.symbol();

        if (hasFourInARow(targetRow, col, player.symbol())) {
            status = GameStatus.WON;
            winner = player;
        } else if (isBoardFull()) {
            status = GameStatus.DRAW;
            winner = null;
        } else {
            currentPlayer = player.opponent();
        }

        return new MoveResult(
            targetRow,
            col,
            player,
            status,
            status == GameStatus.IN_PROGRESS ? currentPlayer : null,
            winner,
            toBoardString()
        );
    }

    private int findDropRow(int col) {
        for (int r = ROWS - 1; r >= 0; r--) {
            if (grid[r][col] == EMPTY) {
                return r;
            }
        }
        return -1;
    }

    private boolean hasFourInARow(int row, int col, char symbol) {
        for (int[] dir : DIRECTIONS) {
            int dr = dir[0];
            int dc = dir[1];
            int count = 1 + countInDirection(row, col, dr, dc, symbol)
                            + countInDirection(row, col, -dr, -dc, symbol);
            if (count >= WIN_LENGTH) {
                return true;
            }
        }
        return false;
    }

    private int countInDirection(int startRow, int startCol, int dr, int dc, char symbol) {
        int count = 0;
        int r = startRow + dr;
        int c = startCol + dc;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && grid[r][c] == symbol) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }

    private boolean isBoardFull() {
        for (int c = 0; c < COLS; c++) {
            if (grid[0][c] == EMPTY) {
                return false;
            }
        }
        return true;
    }

    public char[][] getGridCopy() {
        char[][] copy = new char[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(grid[r], 0, copy[r], 0, COLS);
        }
        return copy;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Player getWinner() {
        return winner;
    }
}