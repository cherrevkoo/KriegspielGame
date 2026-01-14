package org.example.kriegspiel;

import org.example.kriegspiel.net.GameStateDTO;
import org.example.kriegspiel.net.client.ClientGameState;
import org.example.kriegspiel.net.client.NetworkClient;
import org.example.kriegspiel.net.server.GameServer;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.function.Supplier;

public class GameFrame extends JFrame {

    private NetworkClient client;
    private GameServer server;

    private final JLabel statusLabel = new JLabel("Статус: не подключено");

    private final ClientGameState localState;
    private final MapPanel mapPanel;
    private Supplier<Integer> myPlayerIndexSupplier;

    public GameFrame() {
        super("Kriegspiel");
        this.localState = new ClientGameState();
        this.mapPanel = new MapPanel(localState);
        initializeUI();
    }

    public GameFrame(ClientGameState state, NetworkClient client, Supplier<Integer> myPlayerIndexSupplier) {
        super("Kriegspiel");
        this.localState = state;
        this.client = client;
        this.myPlayerIndexSupplier = myPlayerIndexSupplier;
        this.mapPanel = new MapPanel(localState);

        localState.setController(client);
        
        initializeUI();
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton hostBtn = new JButton("Создать сервер (host)");
        JButton joinBtn = new JButton("Подключиться (join)");

        top.add(hostBtn);
        top.add(joinBtn);
        top.add(statusLabel);

        add(top, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);

        hostBtn.addActionListener(e -> startHost());
        joinBtn.addActionListener(e -> joinServer());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startHost() {
        if (server != null) {
            statusLabel.setText("Сервер уже запущен");
            return;
        }
        try {
            server = new GameServer(8080, 8, 8);
            server.start();
            statusLabel.setText("Сервер запущен на 8080. Теперь подключитесь как клиент.");
        } catch (Exception ex) {
            statusLabel.setText("Ошибка запуска сервера: " + ex.getMessage());
        }
    }

    private void joinServer() {
        String url = JOptionPane.showInputDialog(this,
                "Введите ws:// или wss:// адрес:",
                "ws://127.0.0.1:8080");

        if (url == null || url.isBlank()) return;

        String name = JOptionPane.showInputDialog(this, "Имя игрока:", "Игрок");
        if (name == null || name.isBlank()) name = "Игрок";

        try {
            client = new NetworkClient(
                    new URI(url),
                    name,
                    this::onState,
                    this::onStatus,
                    () -> SwingUtilities.invokeLater(() ->
                            statusLabel.setText("Подключено. Отправляем JOIN…"))
            );

            client.connect();

        } catch (Exception ex) {
            statusLabel.setText("Ошибка подключения: " + ex.getMessage());
        }
    }

    public void applyState(GameStateDTO dto) {
        SwingUtilities.invokeLater(() -> {
            localState.updateFromDTO(dto);

            if (myPlayerIndexSupplier != null) {
                localState.setMyPlayerIndex(myPlayerIndexSupplier.get());
            } else if (client != null) {
                localState.setMyPlayerIndex(client.getMyPlayerIndex());
            }
            
            mapPanel.repaint();

            int myIdx = localState.getMyPlayerIndex();
            boolean myTurn = (myIdx > 0 && dto.currentPlayer == myIdx && !dto.gameOver);

            if (dto.gameOver) {
                if (dto.winner == 0) statusLabel.setText("Игра окончена: ничья");
                else statusLabel.setText("Игра окончена. Победил игрок " + dto.winner);
            } else {
                statusLabel.setText(myTurn ? "Ваш ход" : "Ходит противник");
            }
        });
    }

    public void setStatusText(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    private void onState(GameStateDTO dto) {
        applyState(dto);
    }

    private void onStatus(String text) {
        setStatusText(text);
    }
}