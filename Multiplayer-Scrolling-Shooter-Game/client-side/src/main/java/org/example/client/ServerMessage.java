package org.example.client;

import org.example.game.GameState;

import java.util.List;

public class ServerMessage {
    private ServerResponseType type;
    private List<String> lobbies;
    private List<String> players;
    private GameState gameState;
    private String lobbyId; // Yeni eklendi
    private String message;

    public ServerMessage() {
    }

    public ServerResponseType getType() {
        return type;
    }

    public void setType(ServerResponseType type) {
        this.type = type;
    }

    public List<String> getLobbies() {
        return lobbies;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLobbies(List<String> lobbies) {
        this.lobbies = lobbies;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }
}
