package org.example.kriegspiel;

import org.example.kriegspiel.model.Player;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    private final Game game;
    private final MapPanel mapPanel;
    private final JLabel statusLabel;
    private final JLabel player1Label;
    private final JLabel player2Label;

    public GameFrame(Game game) {
        super("Kriegspiel");
        this.game = game;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        int mapWidth = game.getMap().getWidth();
        int mapHeight = game.getMap().getHeight();
        int cellSize = 60;
        int windowWidth = Math.max(1000, mapWidth * cellSize + 100);
        int windowHeight = Math.max(800, mapHeight * cellSize + 200);
        setSize(windowWidth, windowHeight);
        setLocationRelativeTo(null);

        statusLabel = new JLabel();
        mapPanel = new MapPanel(game, this::setStatusText, this::onActionPerformed);

        player1Label = new JLabel();
        player2Label = new JLabel();
        updatePlayerLabels();

        JPanel playersPanel = new JPanel(new GridLayout(1, 2));
        playersPanel.add(player1Label);
        playersPanel.add(player2Label);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(playersPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Помощь");
        
        JMenuItem rulesItem = new JMenuItem("Правила игры");
        rulesItem.addActionListener(e -> showRulesDialog());
        
        JMenuItem unitsInfoItem = new JMenuItem("Информация о юнитах");
        unitsInfoItem.addActionListener(e -> showUnitsInfoDialog());
        
        JMenuItem terrainInfoItem = new JMenuItem("Информация о местности");
        terrainInfoItem.addActionListener(e -> showTerrainInfoDialog());
        
        helpMenu.add(rulesItem);
        helpMenu.add(unitsInfoItem);
        helpMenu.add(terrainInfoItem);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);

        setLayout(new BorderLayout());
        add(mapPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        Player current = game.getCurrentPlayer();
        setStatusText("Ход: " + current.getName()
                + ". Выберите юнита и выполните одно действие (ход или атака).");
    }

    private void setStatusText(String text) {
        statusLabel.setText(text);
    }

    private void onActionPerformed() {
        if (game.isGameOver()) {
            JOptionPane.showMessageDialog(this,
                    "Игра окончена! Победил: " +
                            game.getOpponentPlayer().getName());
            return;
        }

        game.endTurn();
        Player current = game.getCurrentPlayer();

        updatePlayerLabels();
        setStatusText("Ход: " + current.getName()
                + ". Выберите юнита и выполните одно действие (ход или атака).");
        mapPanel.clearSelection();
        mapPanel.repaint();

        if (game.isGameOver()) {
            JOptionPane.showMessageDialog(this,
                    "Игра окончена! Победил: " +
                            game.getOpponentPlayer().getName());
        }
    }

    private void updatePlayerLabels() {
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();
        Player current = game.getCurrentPlayer();

        String p1Text = p1.getName() + " (" + p1.getUnits().size() + " юнитов)";
        String p2Text = p2.getName() + " (" + p2.getUnits().size() + " юнитов)";

        if (current == p1) {
            p1Text = "▶ " + p1Text;
            player1Label.setForeground(Color.BLUE);
            player2Label.setForeground(Color.BLACK);
        } else {
            p2Text = "▶ " + p2Text;
            player1Label.setForeground(Color.BLACK);
            player2Label.setForeground(Color.BLUE);
        }

        player1Label.setText(p1Text);
        player2Label.setText(p2Text);
    }

    private void showRulesDialog() {
        String rules = """
            ════════════════════════════════════════════════════════
                           ПРАВИЛА ИГРЫ КРИНГШПИЛЬ
            ════════════════════════════════════════════════════════
            
            ЦЕЛЬ ИГРЫ
            Уничтожить все юниты противника!
            
            ОСНОВНЫЕ ПРАВИЛА
            
            1. ПОШАГОВАЯ ИГРА
               • Игроки ходят по очереди
               • За один ход можно переместить юнит ИЛИ атаковать
               • После выполнения действия ход автоматически переходит к противнику
            
            2. ТУМАН ВОЙНЫ
               • Вы видите только то, что находится в радиусе обзора ваших юнитов
               • Тёмно-серые клетки - невидимая территория
               • Вражеские юниты видны только когда они в радиусе обзора
            
            3. ДЕЙСТВИЯ ЮНИТОВ
               • ВЫБОР: Кликните по своему юниту (подсветится жёлтым)
               • ХОД: Кликните по зелёной клетке для перемещения
               • АТАКА: Кликните по вражескому юниту в красной зоне
            
            4. ОГЛУШЕНИЕ
               • Оглушённые юниты не могут действовать
               • Индикатор: красный кружок с "Z"
               • Оглушение снимается через несколько ходов
            
            ════════════════════════════════════════════════════════
            
            СОВЕТЫ
            • Используйте кавалерию для разведки (большой радиус обзора)
            • Артиллерия на холмах имеет увеличенную дальность атаки
            • Избегайте болот и ловушек
            • Планируйте ходы заранее - вы не видите всю карту!
            
            ════════════════════════════════════════════════════════
            """;
        
        JTextArea textArea = new JTextArea(rules);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(new Color(240, 240, 240));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Правила игры", 
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showUnitsInfoDialog() {
        String info = """
            ════════════════════════════════════════════════════════
                         ХАРАКТЕРИСТИКИ ЮНИТОВ
            ════════════════════════════════════════════════════════
            
            ПЕХОТА
            • HP: 10
            • Атака: 4
            • Дальность хода: 2 клетки
            • Дальность атаки: 1 клетка
            • Радиус обзора: 3 клетки
            • Особенности: Универсальный юнит, может ходить везде
            
            ────────────────────────────────────────────────────────
            
            КАВАЛЕРИЯ
            • HP: 8
            • Атака: 5
            • Дальность хода: 4 клетки
            • Дальность атаки: 1 клетка
            • Радиус обзора: 4 клетки
            • Особенности: 
              - Быстрая и манёвренная
              - НЕ может входить в ЛЕС
              - В БОЛОТЕ оглушается на 2 хода (вместо 1)
              - Отлична для разведки
            
            ────────────────────────────────────────────────────────
            
            АРТИЛЛЕРИЯ
            • HP: 6
            • Атака: 6
            • Дальность хода: 1 клетка
            • Дальность атаки: 3 клетки
            • Радиус обзора: 3 клетки
            • Особенности:
              - Самая сильная атака
              - Дальнобойная
              - НЕ может входить в БОЛОТО
              - На ХОЛМЕ дальность атаки +1 (итого 4 клетки)
              - Медленная, нужна защита
            
            ════════════════════════════════════════════════════════
            
            СРАВНЕНИЕ
            Скорость:     Кавалерия > Пехота > Артиллерия
            Атака:        Артиллерия > Кавалерия > Пехота
            Выживаемость: Пехота > Кавалерия > Артиллерия
            Обзор:        Кавалерия > Пехота = Артиллерия
            
            ════════════════════════════════════════════════════════
            """;
        
        JTextArea textArea = new JTextArea(info);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(new Color(240, 240, 240));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 550));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Информация о юнитах", 
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showTerrainInfoDialog() {
        String info = """
            ════════════════════════════════════════════════════════
                          ТИПЫ МЕСТНОСТИ
            ════════════════════════════════════════════════════════
            
            РАВНИНА - Светло-серый
            • Все юниты могут проходить
            • Никаких эффектов
            
            ────────────────────────────────────────────────────────
            
            ЛЕС - Зелёный
            • Кавалерия НЕ может входить
            • Пехота и артиллерия проходят свободно
            • Может скрывать юнитов от обзора
            
            ────────────────────────────────────────────────────────
            
            ХОЛМ - Коричневый
            • Все юниты могут проходить
            • БОНУС для артиллерии: +1 к дальности атаки
            • Стратегически важная позиция
            
            ────────────────────────────────────────────────────────
            
            БОЛОТО - Голубой/Бирюзовый
            • Артиллерия НЕ может входить
            • Пехота: оглушение на 1 ход
            • Кавалерия: оглушение на 2 хода
            • Опасно! Избегайте или используйте осторожно
            
            ────────────────────────────────────────────────────────
            
            ЛОВУШКИ
            • Невидимы до активации
            • При активации: 3 урона + оглушение на 1 ход
            • Одноразовые (исчезают после срабатывания)
            • Расположены в случайных местах на карте
            
            ════════════════════════════════════════════════════════
            
            СТРАТЕГИЧЕСКИЕ СОВЕТЫ
            • Используйте холмы для артиллерии
            • Лес защищает от кавалерии
            • Болота - ловушки для неосторожных
            • Планируйте маршруты с учётом местности
            
            ════════════════════════════════════════════════════════
            """;
        
        JTextArea textArea = new JTextArea(info);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(new Color(240, 240, 240));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 550));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Информация о местности", 
                JOptionPane.INFORMATION_MESSAGE);
    }
}