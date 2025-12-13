package org.example.kriegspiel;

import org.example.kriegspiel.map.GameMap;
import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.model.Player;
import org.example.kriegspiel.model.unit.Artillery;
import org.example.kriegspiel.model.unit.Cavalry;
import org.example.kriegspiel.model.unit.Infantry;
import org.example.kriegspiel.model.unit.Unit;

public class Game {

    private final GameMap map;
    private final Player player1;
    private final Player player2;
    private Player currentPlayer;
    private boolean actionPerformedThisTurn;

    public Game(int width, int height, String p1Name, String p2Name) {
        this.map = new GameMap(width, height);
        this.player1 = new Player(p1Name);
        this.player2 = new Player(p2Name);
        this.currentPlayer = player1;
        this.actionPerformedThisTurn = false;
        initUnits();
    }

    public Game(int width, int height) {
        this(width, height, "Игрок 1", "Игрок 2");
    }

    private void initUnits() {
        int p1X = 0;
        int p1Y = 0;
        placeUnit(new Infantry(player1), p1X, p1Y);
        placeUnit(new Infantry(player1), p1X + 1, p1Y);
        placeUnit(new Cavalry(player1), p1X, p1Y + 1);
        placeUnit(new Artillery(player1), p1X + 1, p1Y + 1);

        int p2X = map.getWidth() - 2;
        int p2Y = map.getHeight() - 2;
        placeUnit(new Infantry(player2), p2X, p2Y);
        placeUnit(new Infantry(player2), p2X + 1, p2Y);
        placeUnit(new Cavalry(player2), p2X + 1, p2Y - 1);
        placeUnit(new Artillery(player2), p2X, p2Y - 1);
    }

    private void placeUnit(Unit unit, int x, int y) {
        map.placeUnit(unit, x, y);
        unit.getOwner().addUnit(unit);
    }

    public GameMap getMap() {
        return map;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public Player getOpponentPlayer() {
        return currentPlayer == player1 ? player2 : player1;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void endTurn() {
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        actionPerformedThisTurn = false;
        for (Unit u : currentPlayer.getUnits()) {
            u.tickTurn();
        }
    }

    public boolean isActionPerformedThisTurn() {
        return actionPerformedThisTurn;
    }

    public boolean isGameOver() {
        return !player1.hasUnits() || !player2.hasUnits();
    }

    public void moveUnit(Unit unit, int x, int y) {
        if (actionPerformedThisTurn) {
            throw new IllegalArgumentException("Вы уже выполнили действие в этом ходу. Завершите ход.");
        }
        if (unit.getOwner() != currentPlayer) {
            throw new IllegalArgumentException("Это не ваш юнит.");
        }
        if (!unit.canAct()) {
            throw new IllegalArgumentException("Юнит временно не может ходить (пропускает ещё "
                    + unit.getSkipTurnsRemaining() + " ход(ов)).");
        }
        if (!unit.canMoveTo(x, y, map)) {
            throw new IllegalArgumentException("Нельзя переместиться на эту клетку.");
        }

        map.moveUnit(unit, x, y);
        applyCellEffects(unit);
        actionPerformedThisTurn = true;
    }

    public void attack(Unit attacker, Unit target) {
        if (actionPerformedThisTurn) {
            throw new IllegalArgumentException("Вы уже выполнили действие в этом ходу. Завершите ход.");
        }
        if (attacker.getOwner() != currentPlayer) {
            throw new IllegalArgumentException("Это не ваш юнит.");
        }
        if (target.getOwner() == currentPlayer) {
            throw new IllegalArgumentException("Нельзя атаковать своего юнита.");
        }
        if (!attacker.canAct()) {
            throw new IllegalArgumentException("Юнит временно не может атаковать.");
        }
        if (!attacker.canAttack(target.getX(), target.getY(), map)) {
            throw new IllegalArgumentException("Цель вне диапазона атаки.");
        }

        attacker.attack(target);
        if (!target.isAlive()) {
            map.removeUnit(target);
            target.getOwner().removeUnit(target);
        }
        actionPerformedThisTurn = true;
    }

    private void applyCellEffects(Unit unit) {
        int ux = unit.getX();
        int uy = unit.getY();
        TerrainType terrain = map.getTerrainAt(ux, uy);

        if (terrain == TerrainType.SWAMP) {
            if (unit instanceof Cavalry) {
                unit.stunForTurns(2);
            } else {
                unit.stunForTurns(1);
            }
        }

        if (map.hasTrapAt(ux, uy)) {
            unit.damage(3);
            unit.stunForTurns(1);
            map.triggerTrapAt(ux, uy);
        }
    }
}