package org.example.kriegspiel.map;

import org.example.kriegspiel.model.Player;
import org.example.kriegspiel.model.unit.Unit;

import java.util.List;
import java.util.Random;

public class GameMap {

    private final int width;
    private final int height;
    private final Cell[][] cells;
    private final Random random;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new Cell[height][width];
        this.random = new Random();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = new Cell(x, y, TerrainType.PLAIN);
            }
        }

        initTerrain();
        initTraps();
    }

    private void initTerrain() {
        // Случайная генерация terrain
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TerrainType type = TerrainType.PLAIN;
                double rand = random.nextDouble();

                if (rand < 0.20) {
                    type = TerrainType.FOREST;
                }

                else if (rand < 0.35) {
                    type = TerrainType.SWAMP;
                }

                else if (rand < 0.50) {
                    type = TerrainType.HILL;
                }

                boolean isStartZone1 = (x < 3 && y < 3);
                boolean isStartZone2 = (x >= width - 3 && y >= height - 3);
                
                if (isStartZone1 || isStartZone2) {
                    type = TerrainType.PLAIN;
                }
                
                cells[y][x] = new Cell(x, y, type);
            }
        }
    }

    private void initTraps() {
        int numTraps = (width * height) / 20;
        numTraps = Math.max(3, Math.min(numTraps, 15));
        
        int trapsPlaced = 0;
        int attempts = 0;
        int maxAttempts = width * height * 2;
        
        while (trapsPlaced < numTraps && attempts < maxAttempts) {
            attempts++;
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            boolean isStartZone1 = (x < 3 && y < 3);
            boolean isStartZone2 = (x >= width - 3 && y >= height - 3);
            
            if (isStartZone1 || isStartZone2) {
                continue;
            }

            if (cells[y][x].hasTrap()) {
                continue;
            }

            if (cells[y][x].getTerrain() == TerrainType.SWAMP) {
                continue;
            }
            
            setTrapAt(x, y);
            trapsPlaced++;
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