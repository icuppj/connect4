package com.connect4.game;

public enum Player {
    P1('1'),
    P2('2');

    private final char symbol;

    Player(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    public Player opponent() {
        return this == P1 ? P2 : P1;
    }

    public static Player fromSymbol(char c) {
        if (c == '1') return P1;
        if (c == '2') return P2;
        throw new IllegalArgumentException("Invalid player symbol: " + c);
    }
}