package org.example.kriegspiel.net;

public final class Protocol {
    private Protocol() {}

    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_ACTION = "ACTION";
    public static final String TYPE_STATE = "STATE";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_INFO = "INFO";
    public static final String TYPE_ROLE = "ROLE";

    public static final String ACTION_MOVE = "MOVE";
    public static final String ACTION_ATTACK = "ATTACK";
}
