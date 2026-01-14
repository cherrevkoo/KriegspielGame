package org.example.kriegspiel.net.server;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        int w = 12;
        int h = 12;

        if (args.length >= 1) port = Integer.parseInt(args[0]);
        if (args.length >= 3) {
            w = Integer.parseInt(args[1]);
            h = Integer.parseInt(args[2]);
        }

        GameServer server = new GameServer(port, w, h);
        server.start();
        System.out.println("Kriegspiel WebSocket server started on port " + port + " (" + w + "x" + h + ")");
    }
}
