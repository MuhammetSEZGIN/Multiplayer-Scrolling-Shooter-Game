package org.example.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.example.game.GameState;
import com.google.gson.Gson;


public class GameClient {
    private static final String SERVER_ADDRESS = "25.53.113.157";
    private static final int SERVER_PORT = 2323;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;


    private String playerName;
    private String lobbyId;
    private Consumer<List<String>> lobbyUpdateCallback;
    private Consumer<GameState> gameUpdateCallback;
    private  Consumer <String> chatCallback;
    private Consumer<String> lobbyCreatedCallback;
    private Consumer<String> lobbyJoinCallback;
    public GameClient(String playerName) {
        this.playerName = playerName;
    }

    public void start() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(new ServerListener()).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createLobby() {
        try {

            new Thread(new ServerListener()).start();
            sendMessage(new ClientMessage(ClientRequestType.createLobby, null, 0, 0, playerName));
            System.out.println("Lobby creating...");
        } catch ( Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(ClientMessage clientMessage) {
        if (out != null) {
            String message = new Gson().toJson(clientMessage);
            System.out.println("Sending message: " + message);
            out.println(message);
        } else {
            System.out.println("Output stream is null, message not sent.");
        }
    }

    public void requestAvailableLobbies() {
        sendMessage(new ClientMessage(ClientRequestType.getLobbies, null, 0, 0, playerName));
    }
    public void sendJoinLobbyMessage(String lobbyId) {
        sendMessage(new ClientMessage(ClientRequestType.joinLobby, lobbyId, 0, 0, playerName));
    }

    public void startGame() {
        sendMessage(new ClientMessage(ClientRequestType.startGame, lobbyId, 0, 0, playerName));
    }

    public void sendChatMessage(String message) {
        sendMessage(new ClientMessage(ClientRequestType.chat, lobbyId, 0, 0, playerName, message));
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {

                    // Gelen mesajı işleme
                    Gson gson = new Gson();
                    ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

                    switch (serverMessage.getType()) {
                        case lobbyCreated:
                            lobbyId = serverMessage.getLobbyId();
                            if (lobbyCreatedCallback != null) {
                                lobbyCreatedCallback.accept(lobbyId);
                            }
                            break;
                        case chat:
                            if(chatCallback != null){
                                System.out.println("Server chat message: " + serverMessage.getMessage());
                                chatCallback.accept(serverMessage.getMessage());
                            }
                            break;
                        case lobbyUpdate:
                            if (lobbyUpdateCallback != null) {
                                lobbyId = serverMessage.getLobbyId();
                                lobbyUpdateCallback.accept(serverMessage.getPlayers());
                                System.out.println("Server Players in lobby: " + serverMessage.getPlayers());

                            }
                            break;
                        case gameState:
                                if(gameUpdateCallback != null) {
                                    gameUpdateCallback.accept(serverMessage.getGameState());
                                }
                            break;
                        case lobbiesList:
                            if (lobbyUpdateCallback != null) {
                                System.out.println("Server message type: " + serverMessage.getType());
                                System.out.println("Server message lobbyId: " + serverMessage.getLobbies());
                                lobbyUpdateCallback.accept(serverMessage.getLobbies());
                            }
                            break;
                        case lobbyJoinState:
                            if (lobbyJoinCallback != null) {
                                lobbyJoinCallback.accept(serverMessage.getMessage());
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection reset: " + e.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setChatCallback(Consumer<String> chatCallback) {
        this.chatCallback = chatCallback;
    }
    public void setLobbyUpdateCallback(Consumer<List<String>> callback) {
        this.lobbyUpdateCallback = callback;
    }

    public void setLobbyCreatedCallback(Consumer<String> callback) {
        this.lobbyCreatedCallback = callback;
    }
    public void setLobbyJoinCallback(Consumer<String> callback) {this.lobbyJoinCallback = callback;}

    public void setGameUpdateCallback(Consumer<GameState> callback) {
        this.gameUpdateCallback = callback;
    }

    public Consumer<String> getChatCallback() {
        return chatCallback;
    }

    public String getPlayerName() {
        return playerName;
    }


    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }
}
