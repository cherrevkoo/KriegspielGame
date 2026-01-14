package org.example.kriegspiel.net.client;

import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.net.GameStateDTO;
import org.example.kriegspiel.net.UnitDTO;

import java.util.ArrayList;
import java.util.List;

public class ClientGameState {

    private GameStateDTO dto;

    public ClientGameState(GameStateDTO dto) {
        this.dto = dto;
    }

    public void update(GameStateDTO dto) {
        this.dto = dto;
    }

    public GameStateDTO getDto() {
        return dto;
    }

    public int getWidth() { return dto.width; }
    public int getHeight() { return dto.height; }

    public TerrainType getTerrainAt(int x, int y) { return dto.terrain[y][x]; }
    public boolean hasTrapAt(int x, int y) { return dto.traps != null && dto.traps[y][x]; }

    public UnitDTO getUnitAt(int x, int y) {
        for (UnitDTO u : dto.units) {
            if (u.x == x && u.y == y) return u;
        }
        return null;
    }

    public List<UnitDTO> getUnits() { return new ArrayList<>(dto.units); }

    public int getCurrentPlayer() { return dto.currentPlayer; }

    public String getPlayerName(int idx) {
        return idx == 1 ? dto.player1Name : dto.player2Name;
    }

    public int countUnits(int idx) {
        int c = 0;
        for (UnitDTO u : dto.units) if (u.owner == idx) c++;
        return c;
    }

    public boolean isGameOver() { return dto.gameOver; }
    public int getWinner() { return dto.winner; }
}
