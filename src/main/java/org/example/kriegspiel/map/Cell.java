package org.example.kriegspiel.map;

import org.example.kriegspiel.model.unit.Unit;

public class Cell {

    private final int x;
    private final int y;
    private final TerrainType terrain;
    private Unit unit;

    private boolean hasTrap;

    public Cell(int x, int y, TerrainType terrain) {
        this.x = x;
        this.y = y;
        this.terrain = terrain;
        this.hasTrap = false;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public boolean hasTrap() {
        return hasTrap;
    }

    public void setTrap(boolean hasTrap) {
        this.hasTrap = hasTrap;
    }

    public void triggerTrap() {
        this.hasTrap = false;
    }
}