package org.example.kriegspiel;

import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.net.UnitDTO;
import org.example.kriegspiel.net.client.ClientGameState;
import org.example.kriegspiel.net.client.GameController;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class MapPanel extends JPanel {

    private static final int CELL_SIZE = 60;

    private final ClientGameState state;
    private final GameController controller;
    private final Consumer<String> statusConsumer;
    private final Runnable afterActionCallback;
    private final IntSupplier myPlayerIndexSupplier;

    private int selectedX = -1;
    private int selectedY = -1;

    private final Map<TerrainType, BufferedImage> terrainImg = new EnumMap<>(TerrainType.class);
    private final Map<String, BufferedImage> unitImg = new HashMap<>();

    public MapPanel(ClientGameState state,
                    GameController controller,
                    IntSupplier myPlayerIndexSupplier,
                    Consumer<String> statusConsumer,
                    Runnable afterActionCallback) {
        this.state = state;
        this.controller = controller;
        this.myPlayerIndexSupplier = myPlayerIndexSupplier;
        this.statusConsumer = statusConsumer;
        this.afterActionCallback = afterActionCallback;

        setPreferredSize(new Dimension(state.getWidth() * CELL_SIZE, state.getHeight() * CELL_SIZE));

        loadImages();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX() / CELL_SIZE;
                int y = e.getY() / CELL_SIZE;
                if (x < 0 || y < 0 || x >= state.getWidth() || y >= state.getHeight()) return;
                handleClick(x, y);
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int x = e.getX() / CELL_SIZE;
                int y = e.getY() / CELL_SIZE;
                if (x < 0 || y < 0 || x >= state.getWidth() || y >= state.getHeight()) return;
                showCellInfo(x, y);
            }
        });
    }

    private void loadImages() {
        unitImg.put(keyUnit("Infantry", 1), load("/icons/infantry_blue.png"));
        unitImg.put(keyUnit("Cavalry", 1), load("/icons/cavalry_blue.png"));
        unitImg.put(keyUnit("Artillery", 1), load("/icons/artillery_blue.png"));

        unitImg.put(keyUnit("Infantry", 2), load("/icons/infantry_red.png"));
        unitImg.put(keyUnit("Cavalry", 2), load("/icons/cavalry_red.png"));
        unitImg.put(keyUnit("Artillery", 2), load("/icons/artillery_red.png"));
    }

    private BufferedImage load(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (Exception e) {
            return null;
        }
    }

    private String keyUnit(String type, int owner) {
        return type + "#" + owner;
    }

    public void clearSelection() {
        selectedX = -1;
        selectedY = -1;
    }

    private void handleClick(int x, int y) {
        int myIdx = myPlayerIndexSupplier.getAsInt();
        if (myIdx <= 0) {
            statusConsumer.accept("Ожидаем роль от сервера...");
            return;
        }

        if (state.isGameOver()) {
            statusConsumer.accept("Игра окончена.");
            return;
        }

        if (state.getCurrentPlayer() != myIdx) {
            statusConsumer.accept("Сейчас ходит " + state.getPlayerName(state.getCurrentPlayer()));
            return;
        }

        TerrainType terrain = state.getTerrainAt(x, y);
        if (terrain == null) {
            statusConsumer.accept("Клетка скрыта туманом войны.");
            return;
        }

        UnitDTO clicked = state.getUnitAt(x, y);

        if (selectedX == -1 || selectedY == -1) {
            if (clicked == null) {
                statusConsumer.accept("Здесь нет юнита. Выберите своего юнита.");
                return;
            }
            if (clicked.owner != myIdx) {
                statusConsumer.accept("Это не ваш юнит.");
                return;
            }
            if (clicked.skipTurns > 0) {
                statusConsumer.accept("Юнит оглушён на " + clicked.skipTurns + " ход(а).");
                return;
            }
            selectedX = x;
            selectedY = y;
            statusConsumer.accept("Выбран " + clicked.type + " (HP: " + clicked.hp + "). Выберите цель для хода/атаки.");
            repaint();
            return;
        }

        if (selectedX == x && selectedY == y) {
            clearSelection();
            statusConsumer.accept("Выбор снят.");
            repaint();
            return;
        }

        UnitDTO selected = state.getUnitAt(selectedX, selectedY);
        if (selected == null || selected.owner != myIdx) {
            clearSelection();
            statusConsumer.accept("Выбранный ранее юнит недоступен. Выберите снова.");
            repaint();
            return;
        }

        if (clicked != null && clicked.owner != myIdx) {
            controller.requestAttack(selectedX, selectedY, x, y);
            statusConsumer.accept("Запрос атаки отправлен...");
        } else {
            controller.requestMove(selectedX, selectedY, x, y);
            statusConsumer.accept("Запрос перемещения отправлен...");
        }

        clearSelection();
        repaint();

        if (afterActionCallback != null) {
            Timer t = new Timer(200, e -> afterActionCallback.run());
            t.setRepeats(false);
            t.start();
        }
    }

    private void showCellInfo(int x, int y) {
        TerrainType terrain = state.getTerrainAt(x, y);
        if (terrain == null) {
            statusConsumer.accept("Туман войны");
            return;
        }

        String info = getTerrainDescription(terrain);

        UnitDTO unit = state.getUnitAt(x, y);
        if (unit != null) {
            info += " | " + (unit.owner == 1 ? "Игрок 1" : "Игрок 2")
                    + " | " + unit.type + " (HP: " + unit.hp + ")";
            if (unit.skipTurns > 0) {
                info += " | Оглушён: " + unit.skipTurns;
            }
        }

        if (state.hasTrapAt(x, y)) info += " | Ловушка";
        statusConsumer.accept(info);
    }

    private String getTerrainDescription(TerrainType terrain) {
        return switch (terrain) {
            case PLAIN -> "Равнина - все юниты могут проходить";
            case FOREST -> "Лес - кавалерия не может входить";
            case HILL -> "Холм - артиллерия +1 к дальности атаки";
            case SWAMP -> "Болото - оглушает (артиллерия не входит)";
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        for (int y = 0; y < state.getHeight(); y++) {
            for (int x = 0; x < state.getWidth(); x++) {
                int px = x * CELL_SIZE;
                int py = y * CELL_SIZE;

                TerrainType terrain = state.getTerrainAt(x, y);

                if (terrain == null) {
                    g2.setColor(new Color(25, 25, 25));
                    g2.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(px, py, CELL_SIZE, CELL_SIZE);
                    continue;
                }

                BufferedImage timg = terrainImg.get(terrain);
                if (timg != null) {
                    g2.drawImage(timg, px, py, CELL_SIZE, CELL_SIZE, null);
                } else {
                    g2.setColor(getTerrainColor(terrain));
                    g2.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                }

                if (state.hasTrapAt(x, y)) {
                    g2.setColor(Color.RED);
                    g2.drawOval(px + 20, py + 20, 20, 20);
                }

                UnitDTO unit = state.getUnitAt(x, y);
                if (unit != null) {
                    BufferedImage uimg = unitImg.get(keyUnit(unit.type, unit.owner));
                    if (uimg != null) {
                        g2.drawImage(uimg, px, py, CELL_SIZE, CELL_SIZE, null);
                    } else {
                        g2.setColor(unit.owner == 1 ? Color.BLUE : Color.MAGENTA);
                        g2.setFont(new Font("Arial", Font.BOLD, 22));
                        String symbol = unitSymbol(unit.type);
                        g2.drawString(symbol, px + 24, py + 36);
                    }

                    if (unit.skipTurns > 0) {
                        g2.setColor(Color.ORANGE);
                        g2.setFont(new Font("Arial", Font.BOLD, 12));
                        g2.drawString("Z" + unit.skipTurns, px + 5, py + 15);
                    }
                }

                g2.setColor(Color.BLACK);
                g2.drawRect(px, py, CELL_SIZE, CELL_SIZE);
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

    private Color getTerrainColor(TerrainType t) {
        return switch (t) {
            case PLAIN -> new Color(220, 220, 200);
            case FOREST -> new Color(120, 180, 120);
            case HILL -> new Color(200, 180, 120);
            case SWAMP -> new Color(120, 120, 160);
        };
    }

    private String unitSymbol(String type) {
        return switch (type) {
            case "Infantry" -> "I";
            case "Cavalry" -> "C";
            case "Artillery" -> "A";
            default -> "?";
        };
    }
}