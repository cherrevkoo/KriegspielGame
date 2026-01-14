package org.example.kriegspiel.net.server;

import org.example.kriegspiel.Game;
import org.example.kriegspiel.map.GameMap;
import org.example.kriegspiel.model.Player;
import org.example.kriegspiel.model.unit.Unit;
import org.example.kriegspiel.net.GameStateDTO;
import org.example.kriegspiel.net.UnitDTO;

public final class GameStateMapper {
    private GameStateMapper() {}

    public static GameStateDTO fromGame(Game game) {
        GameMap map = game.getMap();
        GameStateDTO dto = new GameStateDTO();

        dto.width = map.getWidth();
        dto.height = map.getHeight();

        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        dto.player1Name = p1.getName();
        dto.player2Name = p2.getName();
        dto.currentPlayer = game.getCurrentPlayer() == p1 ? 1 : 2;

        dto.terrain = new org.example.kriegspiel.map.TerrainType[dto.height][dto.width];
        dto.traps = new boolean[dto.height][dto.width];

        for (int y = 0; y < dto.height; y++) {
            for (int x = 0; x < dto.width; x++) {
                dto.terrain[y][x] = map.getTerrainAt(x, y);
                dto.traps[y][x] = map.hasTrapAt(x, y);
            }
        }

        dto.units.clear();
        int p1Count = 0;
        int p2Count = 0;

        for (int y = 0; y < dto.height; y++) {
            for (int x = 0; x < dto.width; x++) {
                Unit u = map.getUnitAt(x, y);
                if (u == null) continue;

                int ownerIdx = resolveOwnerIndex(u, p1, p2);
                if (ownerIdx == 1) p1Count++;
                else if (ownerIdx == 2) p2Count++;

                dto.units.add(toUnitDTO(u, ownerIdx, x, y));
            }
        }

        dto.gameOver = game.isGameOver();
        if (dto.gameOver) {
            boolean p1Alive = p1Count > 0;
            boolean p2Alive = p2Count > 0;
            if (p1Alive && !p2Alive) dto.winner = 1;
            else if (p2Alive && !p1Alive) dto.winner = 2;
            else dto.winner = 0;
        } else {
            dto.winner = 0;
        }

        return dto;
    }

    private static int resolveOwnerIndex(Unit u, Player p1, Player p2) {
        try {
            Player owner = u.getOwner();
            if (owner == p1) return 1;
            if (owner == p2) return 2;
        } catch (Throwable ignored) {}

        try {
            if (p1.getUnits() != null && p1.getUnits().contains(u)) return 1;
            if (p2.getUnits() != null && p2.getUnits().contains(u)) return 2;
        } catch (Throwable ignored) {}

        return 0;
    }

    private static UnitDTO toUnitDTO(Unit u, int ownerIdx, int x, int y) {
        UnitDTO dto = new UnitDTO();
        dto.type = u.getClass().getSimpleName();
        dto.owner = ownerIdx;
        dto.hp = u.getHp();
        dto.skipTurns = u.getSkipTurnsRemaining();
        dto.x = x;
        dto.y = y;
        return dto;
    }
}