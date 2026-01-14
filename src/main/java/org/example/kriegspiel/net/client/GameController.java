package org.example.kriegspiel.net.client;

public interface GameController {
    void requestMove(int fromX, int fromY, int toX, int toY);
    void requestAttack(int fromX, int fromY, int toX, int toY);
}
