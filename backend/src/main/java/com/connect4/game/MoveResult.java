package com.connect4.game;

public record MoveResult(
    int row,
    int col,
    Player movedBy,
    GameStatus status,
    Player nextPlayer,
    Player winner,
    String boardString
) {}