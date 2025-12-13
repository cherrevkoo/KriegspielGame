package org.example.kriegspiel;

import org.example.kriegspiel.map.GameMap;
import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.model.Player;
import org.example.kriegspiel.model.unit.Unit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class MapPanel extends JPanel {

    private static final int CELL_SIZE = 60;

    private final Game game;
    private final GameMap map;
    private final Consumer<String> statusConsumer;
    private final Runnable onActionPerformed;

    private int selectedX = -1;
    private int selectedY = -1;

    private Image infantryBlue;
    private Image infantryRed;
    private Image cavalryBlue;
    private Image cavalryRed;
    private Image artilleryBlue;
    private Image artilleryRed;

    public MapPanel(Game game, Consumer<String> statusConsumer, Runnable onActionPerformed) {
        this.game = game;
        this.map = game.getMap();
        this.statusConsumer = statusConsumer;
        this.onActionPerformed = onActionPerformed;

        int w = CELL_SIZE * map.getWidth();
        int h = CELL_SIZE * map.getHeight();
        setPreferredSize(new Dimension(w, h));

        infantryBlue = loadIcon("/icons/infantry_blue.png");
        infantryRed = loadIcon("/icons/infantry_red.png");
        cavalryBlue = loadIcon("/icons/cavalry_blue.png");
        cavalryRed = loadIcon("/icons/cavalry_red.png");
        artilleryBlue = loadIcon("/icons/artillery_blue.png");
        artilleryRed = loadIcon("/icons/artillery_red.png");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e.getX(), e.getY());
            }
        });
    }

    private Image loadIcon(String path) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("Не удалось найти ресурс: " + path);
            return null;
        }
        return new ImageIcon(url).getImage();
    }

    public void clearSelection() {
        selectedX = -1;
        selectedY = -1;
    }

    private void handleMouseMove(int mouseX, int mouseY) {
        int x = mouseX / CELL_SIZE;
        int y = mouseY / CELL_SIZE;
        if (!map.isInside(x, y)) {
            return;
        }

        Player current = game.getCurrentPlayer();
        boolean[][] visible = map.getVisibilityFor(current);
        
        Unit unit = map.getUnitAt(x, y);
        if (unit != null && unit.getOwner() == current) {
            visible[y][x] = true;
        }
        
        if (visible[y][x]) {
            TerrainType terrain = map.getTerrainAt(x, y);
            String terrainInfo = getTerrainDescription(terrain);
            
            if (unit != null) {
                String unitInfo = unit.getOwner() == current ? "Ваш " : "Враг: ";
                unitInfo += getUnitTypeName(unit) + " (HP: " + unit.getHp() + ")";
                if (!unit.canAct()) {
                    unitInfo += " [Оглушён на " + unit.getSkipTurnsRemaining() + " ход(ов)]";
                }
                statusConsumer.accept(terrainInfo + " | " + unitInfo);
            } else {
                if (map.hasTrapAt(x, y)) {
                    terrainInfo += " | Ловушка";
                }
                statusConsumer.accept(terrainInfo);
            }
        } else {
            statusConsumer.accept("Туман войны - территория не видна");
        }
    }

    private String getTerrainDescription(TerrainType terrain) {
        return switch (terrain) {
            case PLAIN -> "Равнина - все юниты могут проходить";
            case FOREST -> "Лес - кавалерия не может входить";
            case HILL -> "Холм - артиллерия получает +1 к дальности атаки";
            case SWAMP -> "Болото - оглушение (пехота 1 ход, кавалерия 2 хода), артиллерия не может входить";
        };
    }

    private String getUnitTypeName(Unit unit) {
        char symbol = unit.getSymbol();
        return switch (symbol) {
            case 'I' -> "Пехота";
            case 'C' -> "Кавалерия";
            case 'A' -> "Артиллерия";
            default -> "Юнит";
        };
    }

    private void handleClick(int mouseX, int mouseY) {
        int x = mouseX / CELL_SIZE;
        int y = mouseY / CELL_SIZE;
        if (!map.isInside(x, y)) {
            return;
        }

        if (game.isActionPerformedThisTurn()) {
            statusConsumer.accept("Вы уже выполнили действие в этом ходу. Ход переходит к противнику.");
            return;
        }

        Player current = game.getCurrentPlayer();
        Unit clickedUnit = map.getUnitAt(x, y);

        if (selectedX == -1 && selectedY == -1) {
            if (clickedUnit == null) {
                statusConsumer.accept("Здесь нет юнита. Выберите своего юнита.");
                return;
            }
            if (clickedUnit.getOwner() != current) {
                statusConsumer.accept("Это юнит противника. Сначала выберите своего.");
                return;
            }
            selectedX = x;
            selectedY = y;
            Unit selected = map.getUnitAt(selectedX, selectedY);
            String status = "Выбран юнит на (" + x + "," + y + "). HP: " + selected.getHp();
            if (!selected.canAct()) {
                status += " [Оглушён на " + selected.getSkipTurnsRemaining() + " ход(ов)]";
            }
            status += ". Кликните по цели (клетка/вражеский юнит).";
            statusConsumer.accept(status);
            repaint();
            return;
        }

        Unit selectedUnit = map.getUnitAt(selectedX, selectedY);
        if (selectedUnit == null || selectedUnit.getOwner() != current) {
            clearSelection();
            statusConsumer.accept("Выбранный ранее юнит недоступен. Выберите снова.");
            repaint();
            return;
        }

        if (x == selectedX && y == selectedY) {
            clearSelection();
            statusConsumer.accept("Выбор юнита отменён.");
            repaint();
            return;
        }

        try {
            if (clickedUnit == null) {
                game.moveUnit(selectedUnit, x, y);
                statusConsumer.accept("Юнит перемещён на (" + x + "," + y + "). Ход переходит к противнику...");
                clearSelection();
                repaint();
                Timer timer = new Timer(800, e -> {
                    onActionPerformed.run();
                });
                timer.setRepeats(false);
                timer.start();
            } else {
                int targetHpBefore = clickedUnit.getHp();
                game.attack(selectedUnit, clickedUnit);
                int damage = targetHpBefore - clickedUnit.getHp();
                String msg = "Атака по (" + x + "," + y + ") нанесла " + damage + " урона.";
                if (!clickedUnit.isAlive()) {
                    msg += " Враг уничтожен!";
                } else {
                    msg += " У врага осталось " + clickedUnit.getHp() + " HP.";
                }
                msg += " Ход переходит к противнику...";
                statusConsumer.accept(msg);
                clearSelection();
                repaint();
                Timer timer = new Timer(800, e -> {
                    onActionPerformed.run();
                });
                timer.setRepeats(false);
                timer.start();
            }
        } catch (IllegalArgumentException ex) {
            statusConsumer.accept(ex.getMessage());
            repaint();
        }
    }

    private Image getUnitIcon(Unit unit, boolean friendly) {
        char symbol = unit.getSymbol();
        return switch (symbol) {
            case 'I' -> friendly ? infantryBlue : infantryRed;
            case 'C' -> friendly ? cavalryBlue : cavalryRed;
            case 'A' -> friendly ? artilleryBlue : artilleryRed;
            default -> null;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Player current = game.getCurrentPlayer();
        boolean[][] visible = map.getVisibilityFor(current);
        
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Unit unit = map.getUnitAt(x, y);
                if (unit != null && unit.getOwner() == current) {
                    visible[y][x] = true;
                }
            }
        }

        boolean[][] moveHighlight = new boolean[map.getHeight()][map.getWidth()];
        boolean[][] attackHighlight = new boolean[map.getHeight()][map.getWidth()];
        if (selectedX != -1 && selectedY != -1) {
            Unit selectedUnit = map.getUnitAt(selectedX, selectedY);
            if (selectedUnit != null && selectedUnit.getOwner() == current && selectedUnit.canAct()) {
                for (int y = 0; y < map.getHeight(); y++) {
                    for (int x = 0; x < map.getWidth(); x++) {
                        if (selectedUnit.canMoveTo(x, y, map)) {
                            moveHighlight[y][x] = true;
                        }
                        if (selectedUnit.canAttack(x, y, map)) {
                            Unit target = map.getUnitAt(x, y);
                            if (target != null && target.getOwner() != current) {
                                attackHighlight[y][x] = true;
                            }
                        }
                    }
                }
            }
        }

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int px = x * CELL_SIZE;
                int py = y * CELL_SIZE;

                TerrainType terrain = map.getTerrainAt(x, y);
                if (!visible[y][x]) {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                } else {
                    switch (terrain) {
                        case PLAIN -> g.setColor(new Color(200, 200, 200));
                        case FOREST -> g.setColor(new Color(34, 139, 34));
                        case HILL -> g.setColor(new Color(205, 133, 63));
                        case SWAMP -> g.setColor(new Color(72, 209, 204));
                    }
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);

                    if (moveHighlight[y][x]) {
                        g.setColor(new Color(100, 200, 100, 100));
                        g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    }

                    if (attackHighlight[y][x]) {
                        g.setColor(new Color(200, 100, 100, 150));
                        g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    }

                    Unit unit = map.getUnitAt(x, y);
                    if (unit != null) {
                        boolean friendly = (unit.getOwner() == current);
                        if (!friendly && !visible[y][x]) {
                            continue;
                        }
                        Image img = getUnitIcon(unit, friendly);
                        if (img != null) {
                            int padding = 6;
                            g.drawImage(
                                    img,
                                    px + padding,
                                    py + padding,
                                    CELL_SIZE - 2 * padding,
                                    CELL_SIZE - 2 * padding,
                                    this
                            );
                        } else {
                            g.setColor(friendly ? Color.BLUE : Color.RED);
                            g.setFont(new Font("Arial", Font.BOLD, 24));
                            FontMetrics fm = g.getFontMetrics();
                            String symbol = String.valueOf(unit.getSymbol());
                            int textX = px + (CELL_SIZE - fm.stringWidth(symbol)) / 2;
                            int textY = py + (CELL_SIZE + fm.getAscent()) / 2;
                            g.drawString(symbol, textX, textY);
                        }

                        int maxHp = 10;
                        if (unit instanceof org.example.kriegspiel.model.unit.Infantry) {
                            maxHp = 10;
                        } else if (unit instanceof org.example.kriegspiel.model.unit.Cavalry) {
                            maxHp = 8;
                        } else if (unit instanceof org.example.kriegspiel.model.unit.Artillery) {
                            maxHp = 6;
                        }

                        int hpBarWidth = CELL_SIZE - 8;
                        int hpBarHeight = 4;
                        int hpBarX = px + 4;
                        int hpBarY = py + CELL_SIZE - 8;

                        g.setColor(Color.BLACK);
                        g.fillRect(hpBarX - 1, hpBarY - 1, hpBarWidth + 2, hpBarHeight + 2);

                        double hpPercent = (double) unit.getHp() / maxHp;
                        int hpWidth = (int) (hpBarWidth * hpPercent);
                        if (hpPercent > 0.6) {
                            g.setColor(Color.GREEN);
                        } else if (hpPercent > 0.3) {
                            g.setColor(Color.YELLOW);
                        } else {
                            g.setColor(Color.RED);
                        }
                        g.fillRect(hpBarX, hpBarY, hpWidth, hpBarHeight);

                        if (!unit.canAct()) {
                            g.setColor(new Color(255, 0, 0, 180));
                            g.fillOval(px + CELL_SIZE - 16, py + 2, 14, 14);
                            g.setColor(Color.WHITE);
                            g.setFont(new Font("Arial", Font.BOLD, 10));
                            g.drawString("Z", px + CELL_SIZE - 13, py + 12);
                        }
                    }
                    
                    if (visible[y][x] && map.hasTrapAt(x, y)) {
                        g2.setColor(new Color(200, 0, 0, 150));
                        g2.setStroke(new BasicStroke(2));
                        int trapSize = 8;
                        int trapX = px + 2;
                        int trapY = py + 2;
                        g2.drawLine(trapX, trapY, trapX + trapSize, trapY + trapSize);
                        g2.drawLine(trapX + trapSize, trapY, trapX, trapY + trapSize);
                    }
                }

                g.setColor(Color.BLACK);
                g.drawRect(px, py, CELL_SIZE, CELL_SIZE);
            }
        }

        if (selectedX != -1 && selectedY != -1) {
            int px = selectedX * CELL_SIZE;
            int py = selectedY * CELL_SIZE;
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(px + 2, py + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        }
    }
}