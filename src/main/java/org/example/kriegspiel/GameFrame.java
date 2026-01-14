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
    private JPanel topPanel;
    private JButton hostBtn;
    private JButton joinBtn;
    private JPanel rulesPanel;

    private final ClientGameState localState;
    private final MapPanel mapPanel;
    private Supplier<Integer> myPlayerIndexSupplier;
    private boolean gameStarted = false;

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

        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hostBtn = new JButton("Создать сервер (host)");
        joinBtn = new JButton("Подключиться (join)");

        topPanel.add(hostBtn);
        topPanel.add(joinBtn);
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);

        hostBtn.addActionListener(e -> startHost());
        joinBtn.addActionListener(e -> joinServer());

        rulesPanel = createRulesPanel();
        add(rulesPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createRulesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Правила игры"));
        panel.setPreferredSize(new Dimension(250, 600));
        panel.setBackground(new Color(240, 240, 240));

        Font headerFont = new Font("Arial", Font.BOLD, 14);
        Font textFont = new Font("Arial", Font.PLAIN, 11);

        JLabel infantryHeader = new JLabel("Пехота (Infantry)");
        infantryHeader.setFont(headerFont);
        panel.add(infantryHeader);
        panel.add(createRuleLabel("HP: 10", textFont));
        panel.add(createRuleLabel("Урон: 4", textFont));
        panel.add(createRuleLabel("Движение: 2 клетки", textFont));
        panel.add(createRuleLabel("Атака: 1 клетка", textFont));
        panel.add(createRuleLabel("Видимость: 3 клетки", textFont));
        panel.add(createRuleLabel("Может ходить везде", textFont));
        panel.add(Box.createVerticalStrut(10));

        JLabel cavalryHeader = new JLabel("Кавалерия (Cavalry)");
        cavalryHeader.setFont(headerFont);
        panel.add(cavalryHeader);
        panel.add(createRuleLabel("HP: 8", textFont));
        panel.add(createRuleLabel("Урон: 5", textFont));
        panel.add(createRuleLabel("Движение: 4 клетки", textFont));
        panel.add(createRuleLabel("Атака: 1 клетка", textFont));
        panel.add(createRuleLabel("Видимость: 4 клетки", textFont));
        panel.add(createRuleLabel("Не может входить в лес", textFont));
        panel.add(Box.createVerticalStrut(10));

        JLabel artilleryHeader = new JLabel("Артиллерия (Artillery)");
        artilleryHeader.setFont(headerFont);
        panel.add(artilleryHeader);
        panel.add(createRuleLabel("HP: 6", textFont));
        panel.add(createRuleLabel("Урон: 6", textFont));
        panel.add(createRuleLabel("Движение: 1 клетка", textFont));
        panel.add(createRuleLabel("Атака: 3 клетки", textFont));
        panel.add(createRuleLabel("Атака с холма: 4 клетки", textFont));
        panel.add(createRuleLabel("Видимость: 3 клетки", textFont));
        panel.add(createRuleLabel("Не может входить в болото", textFont));
        panel.add(Box.createVerticalStrut(10));

        JLabel generalHeader = new JLabel("Общие правила");
        generalHeader.setFont(headerFont);
        panel.add(generalHeader);
        panel.add(createRuleLabel("• ЛКМ на врага - атака", textFont));
        panel.add(createRuleLabel("• ЛКМ на пустую клетку - движение", textFont));
        panel.add(createRuleLabel("• ПКМ на врага - атака", textFont));
        panel.add(createRuleLabel("• Болото оглушает на 1-2 хода", textFont));
        panel.add(createRuleLabel("• Ловушки скрыты до активации", textFont));
        panel.add(createRuleLabel("• Туман войны скрывает врагов", textFont));

        return panel;
    }

    private JLabel createRuleLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
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
            boolean wasGameOver = localState.isGameOver();
            
            localState.updateFromDTO(dto);

            if (myPlayerIndexSupplier != null) {
                localState.setMyPlayerIndex(myPlayerIndexSupplier.get());
            } else if (client != null) {
                localState.setMyPlayerIndex(client.getMyPlayerIndex());
            }

            if (!gameStarted && dto.currentPlayer > 0 && !dto.gameOver) {
                gameStarted = true;
                hostBtn.setVisible(false);
                joinBtn.setVisible(false);
                topPanel.revalidate();
                topPanel.repaint();
            }
            
            mapPanel.repaint();

            int myIdx = localState.getMyPlayerIndex();
            boolean myTurn = (myIdx > 0 && dto.currentPlayer == myIdx && !dto.gameOver);

            if (dto.gameOver) {
                if (!wasGameOver) {
                    showGameOverDialog(dto);
                }
                if (dto.winner == 0) {
                    statusLabel.setText("Игра окончена: ничья");
                } else {
                    String winnerName = dto.winner == 1 ? dto.player1Name : dto.player2Name;
                    statusLabel.setText("Игра окончена. Победил: " + winnerName);
                }
            } else {
                statusLabel.setText(myTurn ? "Ваш ход" : "Ходит противник");
            }
        });
    }

    private void showGameOverDialog(GameStateDTO dto) {
        String message;
        String title;
        
        if (dto.winner == 0) {
            title = "Игра окончена";
            message = "<html><div style='text-align: center;'><h1>НИЧЬЯ!</h1><p>Оба игрока проиграли одновременно.</p></div></html>";
        } else {
            String winnerName = dto.winner == 1 ? dto.player1Name : dto.player2Name;
            int myIdx = localState.getMyPlayerIndex();
            boolean isWinner = (dto.winner == myIdx);
            
            title = "Игра окончена";
            if (isWinner) {
                message = "<html><div style='text-align: center;'><h1 style='color: green; font-size: 24px;'>ПОБЕДА!</h1>" +
                          "<p style='font-size: 16px;'>Поздравляем, " + winnerName + "!</p>" +
                          "<p>Вы победили в этой битве!</p></div></html>";
            } else {
                message = "<html><div style='text-align: center;'><h1 style='color: red; font-size: 24px;'>ПОРАЖЕНИЕ</h1>" +
                          "<p style='font-size: 16px;'>Победил: " + winnerName + "</p>" +
                          "<p>Ваши войска потерпели поражение.</p></div></html>";
            }
        }
        
        JLabel messageLabel = new JLabel(message);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JOptionPane.showMessageDialog(
            this,
            messageLabel,
            title,
            JOptionPane.INFORMATION_MESSAGE
        );
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