package org.example.kriegspiel.net.client;

import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.net.GameStateDTO;
import org.example.kriegspiel.net.UnitDTO;

import java.util.ArrayList;
import java.util.List;

public class ClientGameState implements GameController {

    private GameStateDTO dto;
    private int myPlayerIndex = 0;
    private GameController controller;

    public ClientGameState() {
        this.dto = new GameStateDTO();
        this.dto.width = 8;
        this.dto.height = 8;
        this.dto.terrain = new TerrainType[8][8];
        this.dto.traps = new boolean[8][8];
        this.dto.units = new ArrayList<>();
        this.dto.currentPlayer = 1;
        this.dto.gameOver = false;
        this.dto.winner = 0;
    }

    public ClientGameState(GameStateDTO dto) {
        this.dto = dto;
    }

    public void updateFromDTO(GameStateDTO dto) {
        this.dto = dto;
    }

    public void update(GameStateDTO dto) {
        this.dto = dto;
    }

    public void setMyPlayerIndex(int index) {
        this.myPlayerIndex = index;
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public GameStateDTO getDto() {
        return dto;
    }

    public int getWidth() { 
        return dto != null ? dto.width : 0; 
    }
    
    public int getHeight() { 
        return dto != null ? dto.height : 0; 
    }

    public boolean isInside(int x, int y) {
        return dto != null && x >= 0 && x < dto.width && y >= 0 && y < dto.height;
    }

    public TerrainType getTerrainAt(int x, int y) { 
        if (dto == null || !isInside(x, y)) return null;
        return dto.terrain[y][x]; 
    }
    
    public boolean hasTrapAt(int x, int y) { 
        if (dto == null || !isInside(x, y)) return false;
        return dto.traps != null && dto.traps[y][x]; 
    }

    public UnitDTO getUnitAt(int x, int y) {
        if (dto == null || dto.units == null) return null;
        for (UnitDTO u : dto.units) {
            if (u.x == x && u.y == y) return u;
        }
        return null;
    }

    public List<UnitDTO> getUnits() { 
        return dto != null && dto.units != null ? new ArrayList<>(dto.units) : new ArrayList<>(); 
    }

    public int getCurrentPlayer() { 
        return dto != null ? dto.currentPlayer : 0; 
    }

    public int getMyPlayerIndex() {
        return myPlayerIndex;
    }

    public String getPlayerName(int idx) {
        if (dto == null) return "Игрок " + idx;
        return idx == 1 ? dto.player1Name : dto.player2Name;
    }

    public int countUnits(int idx) {
        if (dto == null || dto.units == null) return 0;
        int c = 0;
        for (UnitDTO u : dto.units) if (u.owner == idx) c++;
        return c;
    }

    public boolean isGameOver() { 
        return dto != null && dto.gameOver; 
    }
    
    public int getWinner() { 
        return dto != null ? dto.winner : 0; 
    }

    @Override
    public void requestMove(int fromX, int fromY, int toX, int toY) {
        if (controller != null) {
            controller.requestMove(fromX, fromY, toX, toY);
        }
    }

    @Override
    public void requestAttack(int fromX, int fromY, int toX, int toY) {
        if (controller != null) {
            controller.requestAttack(fromX, fromY, toX, toY);
        }
    }
}
