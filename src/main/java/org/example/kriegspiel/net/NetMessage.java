package org.example.kriegspiel.net;

public class NetMessage {
    public String type;
    public Object payload;

    public NetMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}
