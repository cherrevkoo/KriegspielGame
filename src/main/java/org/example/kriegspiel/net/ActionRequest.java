package org.example.kriegspiel.net;

public class ActionRequest {
    public String action;
    public int fromX;
    public int fromY;
    public int toX;
    public int toY;

    public ActionRequest(String action, int fromX, int fromY, int toX, int toY) {
        this.action = action;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }
}
