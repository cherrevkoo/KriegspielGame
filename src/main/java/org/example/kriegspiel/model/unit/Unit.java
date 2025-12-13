package org.example.kriegspiel.model.unit;

import org.example.kriegspiel.map.GameMap;
import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.model.Player;

public abstract class Unit {

    private final Player owner;
    private int hp;
    private final int attackPower;
    private final int moveRange;
    private final int attackRange;
    private final int visionRange;
    private int x;
    private int y;

    private int skipTurnsRemaining;

    public Unit(Player owner, int hp, int attackPower, int moveRange,
                int attackRange, int visionRange) {
        this.owner = owner;
        this.hp = hp;
        this.attackPower = attackPower;
        this.moveRange = moveRange;
        this.attackRange = attackRange;
        this.visionRange = visionRange;
        this.skipTurnsRemaining = 0;
    }

    public Player getOwner() {
        return owner;
    }

    public int getHp() {
        return hp;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void damage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public int getMoveRange() {
        return moveRange;
    }

    public int getAttackRange() {
        return attackRange;
    }

    public int getVisionRange() {
        return visionRange;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean canAct() {
        return skipTurnsRemaining <= 0 && isAlive();
    }

    public void stunForTurns(int turns) {
        this.skipTurnsRemaining = Math.max(this.skipTurnsRemaining, turns);
    }

    public void tickTurn() {
        if (skipTurnsRemaining > 0) {
            skipTurnsRemaining--;
        }
    }

    public int getSkipTurnsRemaining() {
        return skipTurnsRemaining;
    }

    protected boolean canEnterTerrain(TerrainType terrain) {
        return true;
    }

    public boolean canMoveTo(int targetX, int targetY, GameMap map) {
        if (!map.isInside(targetX, targetY)) return false;
        if (map.getUnitAt(targetX, targetY) != null) return false;
        if (!canAct()) return false;

        int dx = Math.abs(targetX - x);
        int dy = Math.abs(targetY - y);
        if (dx + dy > moveRange) return false;

        TerrainType terrain = map.getTerrainAt(targetX, targetY);
        return canEnterTerrain(terrain);
    }

    public boolean canAttack(int targetX, int targetY, GameMap map) {
        if (!canAct()) return false;

        int dx = Math.abs(targetX - x);
        int dy = Math.abs(targetY - y);
        int range = attackRange;

        if (this instanceof Artillery) {
            TerrainType here = map.getTerrainAt(x, y);
            if (here == TerrainType.HILL) {
                range += 1;
            }
        }

        return dx + dy <= range;
    }

    public void attack(Unit other) {
        other.damage(attackPower);
    }

    public abstract char getSymbol();
}