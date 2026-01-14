package org.example.kriegspiel;

import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.net.UnitDTO;
import org.example.kriegspiel.net.client.ClientGameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MapPanel extends JPanel {

    private static final int CELL_SIZE = 60;

    private final ClientGameState state;

    private int selectedX = -1;
    private int selectedY = -1;

    public MapPanel(ClientGameState state) {
        this.state = state;

        setPreferredSize(new Dimension(state.getWidth() * CELL_SIZE, state.getHeight() * CELL_SIZE));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX() / CELL_SIZE, e.getY() / CELL_SIZE, e);
            }
        });
    }

    private void handleClick(int x, int y, MouseEvent e) {
        if (!state.isInside(x, y)) return;

        int myIdx = state.getMyPlayerIndex();
        if (myIdx <= 0) return;

        boolean myTurn = state.getCurrentPlayer() == myIdx && !state.isGameOver();
        if (!myTurn) return;

        UnitDTO clicked = state.getUnitAt(x, y);

        if (selectedX == -1) {
            if (clicked != null && clicked.owner == myIdx) {
                selectedX = x;
                selectedY = y;
                repaint();
            }
            return;
        }

        UnitDTO selected = state.getUnitAt(selectedX, selectedY);
        if (selected == null) {
            selectedX = -1;
            selectedY = -1;
            repaint();
            return;
        }

        if (SwingUtilities.isLeftMouseButton(e)) {
            // move
            state.requestMove(selectedX, selectedY, x, y);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            if (clicked != null && clicked.owner != myIdx) {
                state.requestAttack(selectedX, selectedY, x, y);
            }
        }

        selectedX = -1;
        selectedY = -1;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = state.getWidth();
        int h = state.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                drawCell(g, x, y);
            }
        }

        g.setColor(Color.DARK_GRAY);
        for (int x = 0; x <= w; x++) g.drawLine(x * CELL_SIZE, 0, x * CELL_SIZE, h * CELL_SIZE);
        for (int y = 0; y <= h; y++) g.drawLine(0, y * CELL_SIZE, w * CELL_SIZE, y * CELL_SIZE);
    }

    private void drawCell(Graphics g, int x, int y) {
        TerrainType t = state.getTerrainAt(x, y);

        if (t == null) {
            g.setColor(new Color(40, 40, 40)); // Темный туман войны
            g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            return;
        }

        switch (t) {
            case PLAIN -> g.setColor(new Color(170, 210, 140));
            case FOREST -> g.setColor(new Color(60, 140, 70));
            case SWAMP -> g.setColor(new Color(120, 120, 90));
            case HILL -> g.setColor(new Color(150, 130, 100));
            default -> g.setColor(new Color(170, 210, 140));
        }
        g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        if (state.hasTrapAt(x, y)) {
            g.setColor(Color.BLACK);
            g.drawString("TRAP", x * CELL_SIZE + 5, y * CELL_SIZE + 15);
        }

        UnitDTO u = state.getUnitAt(x, y);
        if (u != null) {
            drawUnit(g, u, x, y);
        }

        if (x == selectedX && y == selectedY) {
            g.setColor(Color.YELLOW);
            ((Graphics2D) g).setStroke(new BasicStroke(3));
            g.drawRect(x * CELL_SIZE + 2, y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4);
            ((Graphics2D) g).setStroke(new BasicStroke(1));
        }
    }

    private void drawUnit(Graphics g, UnitDTO u, int x, int y) {
        int myIdx = state.getMyPlayerIndex();

        // цвет зависит от владельца
        if (u.owner == 1) g.setColor(new Color(60, 80, 200));
        else g.setColor(new Color(200, 70, 70));

        int pad = 10;
        g.fillOval(x * CELL_SIZE + pad, y * CELL_SIZE + pad, CELL_SIZE - 2 * pad, CELL_SIZE - 2 * pad);

        g.setColor(Color.WHITE);
        String label = u.type.substring(0, Math.min(3, u.type.length())).toUpperCase();
        g.drawString(label, x * CELL_SIZE + 20, y * CELL_SIZE + 33);

        // HP
        g.setColor(Color.BLACK);
        g.drawString("HP:" + u.hp, x * CELL_SIZE + 5, y * CELL_SIZE + CELL_SIZE - 5);

        // маркер "ваш/враг"
        if (myIdx > 0) {
            g.setColor(Color.BLACK);
            g.drawString(u.owner == myIdx ? "YOU" : "EN", x * CELL_SIZE + 5, y * CELL_SIZE + 15);
        }
    }
}