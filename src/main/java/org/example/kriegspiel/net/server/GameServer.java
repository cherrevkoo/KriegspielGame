package org.example.kriegspiel.net.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.kriegspiel.Game;
import org.example.kriegspiel.map.GameMap;
import org.example.kriegspiel.model.Player;
import org.example.kriegspiel.model.unit.Unit;
import org.example.kriegspiel.net.*;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer extends WebSocketServer {

    private final Gson gson = new GsonBuilder().create();

    private final int width;
    private final int height;

    /**
     * WebSocketServer вызывает колбэки из разных потоков, поэтому используем thread-safe map.
     */
    private final Map<WebSocket, Integer> playerIndexByConn = new ConcurrentHashMap<>();

    private String p1Name = null;
    private String p2Name = null;

    private Game game = null;

    public GameServer(int port, int width, int height) {
        super(new InetSocketAddress(port));
        this.width = width;
        this.height = height;
    }

    @Override
    public void onStart() {
        System.out.println("GameServer started on " + getAddress());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("CLIENT CONNECTED: " + conn.getRemoteSocketAddress());
        sendInfo(conn, "Подключено. Отправьте JOIN (введите имя/подключитесь через клиент).");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("MSG FROM " + conn.getRemoteSocketAddress() + ": " + message);
        try {
            NetMessage msg = gson.fromJson(message, NetMessage.class);
            if (msg == null || msg.type == null) return;

            switch (msg.type) {
                case Protocol.TYPE_JOIN -> handleJoin(conn, msg);
                case Protocol.TYPE_ACTION -> handleAction(conn, msg);
                default -> sendError(conn, "Неизвестный тип сообщения: " + msg.type);
            }
        } catch (Exception ex) {
            sendError(conn, "Ошибка обработки запроса: " + ex.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Integer idx = playerIndexByConn.remove(conn);
        System.out.println("CLIENT CLOSED: " + conn.getRemoteSocketAddress()
                + " code=" + code + " reason=" + reason + " idx=" + idx);

        if (idx != null) {
            // В текущей реализации мы не продолжаем матч, если кто-то вышел.
            // Сбрасываем лобби, чтобы можно было переподключиться без перезапуска сервера.
            synchronized (this) {
                game = null;
                p1Name = null;
                p2Name = null;
                playerIndexByConn.clear();
            }
            broadcastInfo("Игрок отключился. Матч остановлен. Переподключитесь, чтобы начать заново.");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("SERVER ERROR: " + (conn != null ? conn.getRemoteSocketAddress() : "null")
                + " err=" + ex.getMessage());
        if (conn != null) {
            sendError(conn, "Сетевая ошибка: " + ex.getMessage());
        }
    }

    private synchronized void handleJoin(WebSocket conn, NetMessage msg) {
        if (playerIndexByConn.containsKey(conn)) {
            sendError(conn, "Вы уже присоединились.");
            return;
        }

        JoinRequest join = gson.fromJson(gson.toJsonTree(msg.payload), JoinRequest.class);
        String name = (join == null || join.playerName == null || join.playerName.isBlank())
                ? "Игрок" : join.playerName.trim();

        if (p1Name == null) {
            p1Name = name;
            playerIndexByConn.put(conn, 1);

            conn.send(gson.toJson(new NetMessage(Protocol.TYPE_ROLE, new RoleMessage(1))));
            sendInfo(conn, "Вы присоединились как Игрок 1. Ждём второго игрока...");
            broadcastInfo("Игрок 1: " + p1Name);

        } else if (p2Name == null) {
            p2Name = name;
            playerIndexByConn.put(conn, 2);

            conn.send(gson.toJson(new NetMessage(Protocol.TYPE_ROLE, new RoleMessage(2))));
            sendInfo(conn, "Вы присоединились как Игрок 2. Запускаем игру...");
            broadcastInfo("Игрок 2: " + p2Name);

        } else {
            sendError(conn, "Лобби заполнено (2 игрока).");
            conn.close();
            return;
        }

        if (p1Name != null && p2Name != null && game == null) {
            game = new Game(width, height, p1Name, p2Name);
            broadcastInfo("Игра началась! Ход: " + game.getCurrentPlayer().getName());
            broadcastState();
        } else if (game != null) {
            sendState(conn);
        }
    }

    private synchronized void handleAction(WebSocket conn, NetMessage msg) {
        if (game == null) {
            sendError(conn, "Игра ещё не началась. Ждём подключения 2 игроков.");
            return;
        }

        Integer playerIdx = playerIndexByConn.get(conn);
        if (playerIdx == null) {
            sendError(conn, "Сначала отправьте JOIN.");
            return;
        }

        ActionRequest action = gson.fromJson(gson.toJsonTree(msg.payload), ActionRequest.class);
        if (action == null || action.action == null) {
            sendError(conn, "Некорректный ACTION.");
            return;
        }

        Player current = game.getCurrentPlayer();
        int currentIdx = (current == game.getPlayer1()) ? 1 : 2;

        if (playerIdx != currentIdx) {
            sendError(conn, "Сейчас ходит другой игрок.");
            return;
        }

        GameMap map = game.getMap();
        Unit fromUnit = map.getUnitAt(action.fromX, action.fromY);
        if (fromUnit == null) {
            sendError(conn, "В выбранной клетке нет юнита.");
            return;
        }

        try {
            switch (action.action) {
                case Protocol.ACTION_MOVE -> {
                    game.moveUnit(fromUnit, action.toX, action.toY);
                    game.endTurn();
                    broadcastState();
                }
                case Protocol.ACTION_ATTACK -> {
                    Unit target = map.getUnitAt(action.toX, action.toY);
                    if (target == null) throw new IllegalArgumentException("Цель для атаки отсутствует.");
                    game.attack(fromUnit, target);
                    game.endTurn();
                    broadcastState();
                }
                default -> sendError(conn, "Неизвестное действие: " + action.action);
            }
        } catch (Exception ex) {
            sendError(conn, ex.getMessage());
        }
    }

    private synchronized void broadcastState() {
        // keySet() у ConcurrentHashMap безопасен, но берём snapshot, чтобы избежать
        // сюрпризов при отключениях в середине рассылки.
        for (WebSocket c : new ArrayList<>(playerIndexByConn.keySet())) {
            sendState(c);
        }
    }

    private void sendState(WebSocket conn) {
        if (game == null) {
            sendInfo(conn, "Ожидаем второго игрока...");
            return;
        }
        GameStateDTO dto = GameStateMapper.fromGame(game);
        conn.send(gson.toJson(new NetMessage(Protocol.TYPE_STATE, dto)));
    }

    private void sendError(WebSocket conn, String message) {
        conn.send(gson.toJson(new NetMessage(Protocol.TYPE_ERROR, new ErrorMessage(message))));
    }

    private void sendInfo(WebSocket conn, String message) {
        conn.send(gson.toJson(new NetMessage(Protocol.TYPE_INFO, new InfoMessage(message))));
    }

    private void broadcastInfo(String message) {
        broadcast(gson.toJson(new NetMessage(Protocol.TYPE_INFO, new InfoMessage(message))));
    }
}