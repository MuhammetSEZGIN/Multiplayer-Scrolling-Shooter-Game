package org.example.server;


public class ClientMessage {
    private String type;
    private String lobbyId;
    private double x;
    private double y;
    private String playerName;
    private String Message; // Yeni eklendi



    public ClientMessage(String type, String lobbyId, double x, double y, String playerName, String Message) {
        this.type = type;
        this.lobbyId = lobbyId;
        this.x = x;
        this.y = y;
        this.playerName = playerName;
        this.Message = Message; // Yeni eklendi
    }
    public ClientMessage(String type, String lobbyId, double x, double y, String playerName) {
        this.type = type;
        this.lobbyId = lobbyId;
        this.x = x;
        this.y = y;
        this.playerName = playerName;
        this.Message = "No message"; // Yeni eklendi
    }


    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }
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
