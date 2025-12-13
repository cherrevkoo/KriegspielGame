package org.example.kriegspiel;

import javax.swing.*;

public class GuiApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String p1 = JOptionPane.showInputDialog(
                    null,
                    "Имя первого игрока:",
                    "Настройка игры",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (p1 == null || p1.isBlank()) {
                p1 = "Игрок 1";
            }

            String p2 = JOptionPane.showInputDialog(
                    null,
                    "Имя второго игрока:",
                    "Настройка игры",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (p2 == null || p2.isBlank()) {
                p2 = "Игрок 2";
            }

            Game game = new Game(12, 12, p1, p2);
            GameFrame frame = new GameFrame(game);
            frame.setVisible(true);
        });
    }
}