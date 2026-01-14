package org.example.kriegspiel;

import org.example.kriegspiel.map.TerrainType;
import org.example.kriegspiel.net.GameStateDTO;
import org.example.kriegspiel.net.client.ClientGameState;
import org.example.kriegspiel.net.client.NetworkClient;
import org.example.kriegspiel.net.server.GameServer;

import javax.swing.*;
import java.net.URI;

public class GuiApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String[] options = {"Создать сервер (host)", "Подключиться (join)"};
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Выберите режим:",
                    "Крингшпиль (WebSocket)",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == JOptionPane.CLOSED_OPTION) return;

            String myName = JOptionPane.showInputDialog(null, "Ваше имя:", "Игрок");
            if (myName == null || myName.isBlank()) myName = "Игрок";

            int port = 8080;
            String url = "ws://localhost:8080";

            if (choice == 0) {
                String portStr = JOptionPane.showInputDialog(null, "Порт сервера:", "8080");
                if (portStr != null && !portStr.isBlank()) {
                    try { port = Integer.parseInt(portStr.trim()); } catch (Exception ignored) {}
                }
                url = "ws://localhost:" + port;

                try {
                    GameServer server = new GameServer(port, 12, 12);
                    server.start();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Не удалось запустить сервер: " + ex.getMessage());
                    return;
                }
            } else {
                String urlStr = JOptionPane.showInputDialog(null,
                        "Адрес сервера (пример: ws://IP:8080 или wss://...):",
                        url);
                if (urlStr == null || urlStr.isBlank()) return;
                url = urlStr.trim();
            }

            GameStateDTO dummy = new GameStateDTO();
            dummy.width = 12;
            dummy.height = 12;
            dummy.player1Name = "Игрок 1";
            dummy.player2Name = "Игрок 2";
            dummy.currentPlayer = 1;
            dummy.terrain = new TerrainType[dummy.height][dummy.width];
            dummy.traps = new boolean[dummy.height][dummy.width];
            for (int y = 0; y < dummy.height; y++) {
                for (int x = 0; x < dummy.width; x++) {
                    dummy.terrain[y][x] = TerrainType.PLAIN;
                    dummy.traps[y][x] = false;
                }
            }

            ClientGameState state = new ClientGameState(dummy);

            try {
                final GameFrame[] frameHolder = new GameFrame[1];

                NetworkClient client = new NetworkClient(
                        URI.create(url),
                        myName,
                        dto -> SwingUtilities.invokeLater(() -> {
                            if (frameHolder[0] != null) frameHolder[0].applyState(dto);
                        }),
                        text -> SwingUtilities.invokeLater(() -> {
                            if (frameHolder[0] != null) frameHolder[0].setStatusText(text);
                        }),
                        () -> SwingUtilities.invokeLater(() -> {
                            if (frameHolder[0] != null) frameHolder[0].setStatusText("Подключено. Ожидаем начала игры...");
                        })
                );

                GameFrame frame = new GameFrame(state, client, client::getMyPlayerIndex);
                frameHolder[0] = frame;

                client.setConnectionLostTimeout(10);
                client.connect();

                frame.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Ошибка подключения: " + ex.getMessage());
            }
        });
    }
}
