package org.example.client;

public class ClientMessage {
    private String type;
    private String lobbyId;
    private double x;
    private double y;
    private String playerName;

    public ClientMessage(String type, String lobbyId, double x, double y, String playerName) {
        this.type = type;
        this.lobbyId = lobbyId;
        this.x = x;
        this.y = y;
        this.playerName = playerName;
    }

    // Getters and setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
