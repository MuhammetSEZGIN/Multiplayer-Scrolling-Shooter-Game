package org.example;

import org.example.server.GameServer;

public class  Main {
    public static void main(String[] args) {
        GameServer gameServer = new GameServer();
        gameServer.start();
    }
}