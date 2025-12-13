package org.example.kriegspiel.model.unit;

import org.example.kriegspiel.model.Player;

public class Infantry extends Unit {

    public Infantry(Player owner) {
        super(owner, 10, 4, 2, 1, 3);
    }

    @Override
    public char getSymbol() {
        return 'I';
    }
}