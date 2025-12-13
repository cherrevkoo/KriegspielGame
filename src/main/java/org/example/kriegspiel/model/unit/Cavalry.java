package org.example.kriegspiel.model.unit;

import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.model.Player;

public class Cavalry extends Unit {

    public Cavalry(Player owner) {
        super(owner, 8, 5, 4, 1, 4);
    }

    @Override
    protected boolean canEnterTerrain(TerrainType terrain) {
        return terrain != TerrainType.FOREST;
    }

    @Override
    public char getSymbol() {
        return 'C';
    }
}