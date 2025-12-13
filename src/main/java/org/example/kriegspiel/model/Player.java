package org.example.kriegspiel.model;

import org.example.kriegspiel.model.unit.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {

    private final String name;
    private final List<Unit> units = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }

    public void addUnit(Unit unit) {
        units.add(unit);
    }

    public void removeUnit(Unit unit) {
        units.remove(unit);
    }

    public boolean hasUnits() {
        return !units.isEmpty();
    }

    public String getName() {
        return name;
    }

    public List<Unit> getUnits() {
        return Collections.unmodifiableList(units);
    }
}