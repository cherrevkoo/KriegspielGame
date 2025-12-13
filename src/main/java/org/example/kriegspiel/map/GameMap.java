package org.example.kriegspiel.map;

import org.example.kriegspiel.model.Player;
import org.example.kriegspiel.model.unit.Unit;

import java.util.List;

public class GameMap {

    private final int width;
    private final int height;
    private final Cell[][] cells;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new Cell[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = new Cell(x, y, TerrainType.PLAIN);
            }
        }

        initTerrain();
        initTraps();
    }

    private void initTerrain() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TerrainType type = TerrainType.PLAIN;
                
                if ((x + y) % 5 == 0 && (x + y) % 3 != 0) {
                    type = TerrainType.FOREST;
                }
                else if (x > 2 && x < width - 3 && y > 2 && y < height - 3 && 
                         (x * y) % 11 == 0) {
                    type = TerrainType.FOREST;
                }
                else if ((x == 3 || x == 4) && (y == 3 || y == 4 || y == height - 4)) {
                    type = TerrainType.SWAMP;
                }
                else if ((y == 3 || y == 4) && (x == width - 4 || x == width - 5)) {
                    type = TerrainType.SWAMP;
                }
                else if (y == height / 2 || y == height / 2 - 1 || y == height / 2 + 1) {
                    type = TerrainType.HILL;
                }
                else if (x == width / 2 || x == width / 2 - 1 || x == width / 2 + 1) {
                    type = TerrainType.HILL;
                }
                
                cells[y][x] = new Cell(x, y, type);
            }
        }
    }

    private void initTraps() {
        int centerX = width / 2;
        int centerY = height / 2;
        
        setTrapAt(centerX, centerY);
        setTrapAt(centerX - 1, centerY + 1);
        setTrapAt(centerX + 1, centerY - 1);
        
        if (width >= 8 && height >= 8) {
            setTrapAt(2, 5);
            setTrapAt(5, 2);
            setTrapAt(width - 3, height - 6);
            setTrapAt(width - 6, height - 3);
        }
    }

    private void setTrapAt(int x, int y) {
        if (isInside(x, y)) {
            cells[y][x].setTrap(true);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Unit getUnitAt(int x, int y) {
        if (!isInside(x, y)) return null;
        return cells[y][x].getUnit();
    }

    public void placeUnit(Unit unit, int x, int y) {
        if (!isInside(x, y)) throw new IllegalArgumentException("Координаты вне карты");
        cells[y][x].setUnit(unit);
        unit.setPosition(x, y);
    }

    public void moveUnit(Unit unit, int newX, int newY) {
        if (!isInside(newX, newY)) throw new IllegalArgumentException("Координаты вне карты");
        cells[unit.getY()][unit.getX()].setUnit(null);
        cells[newY][newX].setUnit(unit);
        unit.setPosition(newX, newY);
    }

    public void removeUnit(Unit unit) {
        if (isInside(unit.getX(), unit.getY())) {
            cells[unit.getY()][unit.getX()].setUnit(null);
        }
    }

    public TerrainType getTerrainAt(int x, int y) {
        if (!isInside(x, y)) return TerrainType.PLAIN;
        return cells[y][x].getTerrain();
    }

    public boolean hasTrapAt(int x, int y) {
        if (!isInside(x, y)) return false;
        return cells[y][x].hasTrap();
    }

    public void triggerTrapAt(int x, int y) {
        if (!isInside(x, y)) return;
        cells[y][x].triggerTrap();
    }

    public boolean[][] getVisibilityFor(Player player) {
        boolean[][] visible = new boolean[height][width];
        List<Unit> units = player.getUnits();

        for (Unit u : units) {
            int vision = u.getVisionRange();
            int ux = u.getX();
            int uy = u.getY();

            for (int dy = -vision; dy <= vision; dy++) {
                for (int dx = -vision; dx <= vision; dx++) {
                    int tx = ux + dx;
                    int ty = uy + dy;
                    if (!isInside(tx, ty)) continue;
                    int manhattan = Math.abs(dx) + Math.abs(dy);
                    if (manhattan <= vision) {
                        visible[ty][tx] = true;
                    }
                }
            }

            if (isInside(ux, uy)) {
                visible[uy][ux] = true;
            }
        }
        return visible;
    }
}