package org.example.kriegspiel;

import org.example.kriegspiel.net.GameStateDTO;
import org.example.kriegspiel.net.client.ClientGameState;
import org.example.kriegspiel.net.client.GameController;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    private final ClientGameState state;
    private final MapPanel mapPanel;
    private final JLabel statusLabel;
    private final JLabel player1Label;
    private final JLabel player2Label;

    private final GameController controller;
    private final java.util.function.IntSupplier myPlayerIndexSupplier;

    public GameFrame(ClientGameState state,
                     GameController controller,
                     java.util.function.IntSupplier myPlayerIndexSupplier) {
        super("Крингшпиль (WebSocket)");

        this.state = state;
        this.controller = controller;
        this.myPlayerIndexSupplier = myPlayerIndexSupplier;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // top panel (players)
        JPanel top = new JPanel(new GridLayout(1, 2));
        player1Label = new JLabel("", SwingConstants.CENTER);
        player2Label = new JLabel("", SwingConstants.CENTER);
        player1Label.setFont(new Font("Arial", Font.BOLD, 16));
        player2Label.setFont(new Font("Arial", Font.BOLD, 16));
        top.add(player1Label);
        top.add(player2Label);
        add(top, BorderLayout.NORTH);

        // map
        mapPanel = new MapPanel(state, controller, myPlayerIndexSupplier, this::setStatusText, this::onAfterAction);
        add(mapPanel, BorderLayout.CENTER);

        // bottom bar
        JPanel bottom = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Подключение...");
        bottom.add(statusLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton rulesBtn = new JButton("Правила");
        rulesBtn.addActionListener(e -> showRulesDialog());
        buttons.add(rulesBtn);

        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        updatePlayerLabels();
    }

    public void applyState(GameStateDTO dto) {
        state.update(dto);


        updatePlayerLabels();

        if (dto.gameOver) {
            String winnerName = state.getPlayerName(dto.winner);
            JOptionPane.showMessageDialog(this, "Игра окончена! Победил: " + winnerName);
        }

        mapPanel.repaint();

        int myIdx = myPlayerIndexSupplier.getAsInt();
        if (myIdx > 0) {
            if (state.getCurrentPlayer() == myIdx) {
                setStatusText("Ваш ход. Выберите юнита и выполните действие (ход или атака).");
            } else {
                setStatusText("Ход противника: " + state.getPlayerName(state.getCurrentPlayer()));
            }
        }
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    private void updatePlayerLabels() {
        String p1Text = state.getPlayerName(1) + " (" + state.countUnits(1) + " юнитов)";
        String p2Text = state.getPlayerName(2) + " (" + state.countUnits(2) + " юнитов)";

        if (state.getCurrentPlayer() == 1) {
            p1Text = "▶ " + p1Text;
            player1Label.setForeground(Color.BLUE);
            player2Label.setForeground(Color.BLACK);
        } else {
            p2Text = "▶ " + p2Text;
            player1Label.setForeground(Color.BLACK);
            player2Label.setForeground(Color.BLUE);
        }

        int myIdx = myPlayerIndexSupplier.getAsInt();
        if (myIdx == 1) {
            p1Text += " (Вы)";
        } else if (myIdx == 2) {
            p2Text += " (Вы)";
        }

        player1Label.setText(p1Text);
        player2Label.setText(p2Text);
    }

    private void onAfterAction() {
        // На сервере ход завершается автоматически после успешного MOVE/ATTACK.
        // Здесь просто уберём выделение.
        mapPanel.clearSelection();
        mapPanel.repaint();
    }

    private void showRulesDialog() {
        String rules = """
            ════════════════════════════════════════════════════════
                           ПРАВИЛА ИГРЫ КРИНГШПИЛЬ
            ════════════════════════════════════════════════════════

            ЦЕЛЬ ИГРЫ
            Уничтожить все юниты противника!

            ХОД
            В свой ход выберите одного своего юнита и выполните ОДНО действие:
            • перемещение, или
            • атака

            ТЕРРЕЙН
            • Равнина: доступна всем
            • Лес: кавалерия не может входить
            • Болото: оглушает; артиллерия не может входить
            • Холм: артиллерия +1 к дальности атаки

            ВЕБ-ВЕРСИЯ (WebSocket)
            Сервер — авторитет: он проверяет ходы/атаки и рассылает состояние обоим игрокам.
            """;

        JTextArea area = new JTextArea(rules);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(650, 450));
        JOptionPane.showMessageDialog(this, scroll, "Правила", JOptionPane.INFORMATION_MESSAGE);
    }
}
