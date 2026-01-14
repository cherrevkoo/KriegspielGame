package org.example.kriegspiel.net;

import org.example.kriegspiel.map.TerrainType;
import java.util.ArrayList;
import java.util.List;

public class GameStateDTO {
    public int width;
    public int height;

    public String player1Name;
    public String player2Name;

    public int currentPlayer; // 1 или 2

    public TerrainType[][] terrain;

    public boolean[][] traps;

    public List<UnitDTO> units = new ArrayList<>();

    public boolean gameOver;
    public int winner;
}
