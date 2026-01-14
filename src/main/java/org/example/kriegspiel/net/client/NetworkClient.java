package org.example.kriegspiel.net.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.kriegspiel.net.*;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class NetworkClient extends WebSocketClient implements GameController {

    private final Gson gson = new GsonBuilder().create();
    private final String playerName;

    private final Consumer<GameStateDTO> onState;
    private final Consumer<String> onStatus;
    private final Runnable onConnected;
    /**
     * 0 = роль ещё не назначена сервером, 1 или 2 = ваш номер игрока.
     * (MapPanel/GameFrame ожидают именно 0 как «неизвестно».)
     */
    private volatile int myPlayerIndex = 0;

    public int getMyPlayerIndex() {
        return myPlayerIndex;
    }

    public NetworkClient(URI serverUri,
                         String playerName,
                         Consumer<GameStateDTO> onState,
                         Consumer<String> onStatus,
                         Runnable onConnected) {
        super(serverUri);
        this.playerName = playerName;
        this.onState = onState;
        this.onStatus = onStatus;
        this.onConnected = onConnected;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        send(gson.toJson(new NetMessage(Protocol.TYPE_JOIN, new JoinRequest(playerName))));
        if (onConnected != null) onConnected.run();
    }

    @Override
    public void onMessage(String message) {
        try {
            NetMessage msg = gson.fromJson(message, NetMessage.class);
            if (msg == null || msg.type == null) return;

            switch (msg.type) {
                case Protocol.TYPE_STATE -> {
                    GameStateDTO state = gson.fromJson(gson.toJsonTree(msg.payload), GameStateDTO.class);
                    onState.accept(state);
                }
                case Protocol.TYPE_ERROR -> {
                    ErrorMessage err = gson.fromJson(gson.toJsonTree(msg.payload), ErrorMessage.class);
                    onStatus.accept(err.message);
                }
                case Protocol.TYPE_ROLE -> {
                    RoleMessage role = gson.fromJson(gson.toJsonTree(msg.payload), RoleMessage.class);
                    if (role != null) myPlayerIndex = role.playerIndex;
                }
                case Protocol.TYPE_INFO -> {
                    InfoMessage inf = gson.fromJson(gson.toJsonTree(msg.payload), InfoMessage.class);
                    onStatus.accept(inf.message);
                }
                default -> {
                }
            }
        } catch (Exception ex) {
            onStatus.accept("Ошибка обработки сообщения от сервера: " + ex.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        onStatus.accept("Соединение закрыто: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        onStatus.accept("Ошибка сети: " + ex.getMessage());
    }

    @Override
    public void requestMove(int fromX, int fromY, int toX, int toY) {
        send(gson.toJson(new NetMessage(Protocol.TYPE_ACTION,
                new ActionRequest(Protocol.ACTION_MOVE, fromX, fromY, toX, toY))));
    }

    @Override
    public void requestAttack(int fromX, int fromY, int toX, int toY) {
        send(gson.toJson(new NetMessage(Protocol.TYPE_ACTION,
                new ActionRequest(Protocol.ACTION_ATTACK, fromX, fromY, toX, toY))));
    }
}
