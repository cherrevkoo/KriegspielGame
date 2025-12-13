package org.example.kriegspiel.model.unit;

import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.model.Player;

public class Artillery extends Unit {

    public Artillery(Player owner) {
        super(owner, 6, 6, 1, 3, 3);
    }

    @Override
    protected boolean canEnterTerrain(TerrainType terrain) {
        return terrain != TerrainType.SWAMP;
    }

    @Override
    public char getSymbol() {
        return 'A';
    }
}