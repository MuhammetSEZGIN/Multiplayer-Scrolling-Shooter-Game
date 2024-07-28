package org.example.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.game.Game;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class GameServer {
    private static final int PORT = 2323;
    private static final String SERVER_HOSTNAME = "25.53.113.157"; // Or your public IP
    private ExecutorService pool;
    private List<ClientHandler> clients;
    private Map<String, Game> lobbies;
    private ObjectMapper objectMapper;
    private Timer gameUpdateTimer;

    public GameServer() {
        pool = Executors.newFixedThreadPool(20);
        clients = Collections.synchronizedList(new ArrayList<>());
        lobbies = new HashMap<>();
        objectMapper = new ObjectMapper();
        gameUpdateTimer = new Timer(true);
        gameUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateGames();
            }
        }, 0, 1000 / 60); // 60 FPS
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT, 0, InetAddress.getByName(SERVER_HOSTNAME))) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message, String lobbyId) {
        Game lobby = lobbies.get(lobbyId);
        if (lobby != null) {
            synchronized (lobby.getClients()) {
                for (ClientHandler client : lobby.getClients()) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public void processClientMessage(String message, ClientHandler sender) {
        //ClientMessage clientMessage = objectMapper.readValue(message, ClientMessage.class);


        JSONObject clientMessageJson = new JSONObject(message);

        ClientRequestType type = ClientRequestType.valueOf(clientMessageJson.getString("type"));
        String lobbyId= null;

        if(clientMessageJson.has("lobbyId")){
            lobbyId = clientMessageJson.getString("lobbyId");
        }

        int x = clientMessageJson.getInt("x");
        int y = clientMessageJson.getInt("y");
        String playerName = clientMessageJson.getString("playerName");
        String Message = clientMessageJson.getString("Message");
        sender.setPlayerName(playerName);

        ClientMessage clientMessage = new ClientMessage(type, lobbyId, x, y, playerName, Message);


        if (clientMessage.getType().equals(ClientRequestType.createLobby)) {
            createLobby(sender, clientMessage.getPlayerName());
        }
        else if (clientMessage.getType().equals(ClientRequestType.getLobbies)) {
            sendLobbiesList(sender);
        }
        else if (clientMessage.getType().equals(ClientRequestType.joinLobby)) {
            System.out.println(clientMessage.getPlayerName()+" "+clientMessage.getLobbyId());
            joinLobby(clientMessage.getLobbyId(), sender, clientMessage.getPlayerName());
        }
        else if (clientMessage.getLobbyId() != null) {
            Game lobby = lobbies.get(clientMessage.getLobbyId());
            if (lobby != null) {
                lobby.processMessage(clientMessage, sender);
            }

        }

    }

    public void sendChatMassage(String playerName,String message, String lobbyId){
        Game lobby = lobbies.get(lobbyId);
        // server mesajı oluştur
        System.out.println("Chat message: "+ message);
        System.out.println("lobby: "+ lobby.getLobbyId());
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.setType(ServerResponseType.chat);
        serverMessage.setLobbyId(lobbyId);
        message= playerName +": "+ message;
        serverMessage.setMessage(message);

        try {
            broadcast(objectMapper.writeValueAsString(serverMessage), lobbyId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private void createLobby(ClientHandler creator, String playerName) {
        String newLobbyId = UUID.randomUUID().toString();
        Game lobby = new Game(this, newLobbyId);
        lobby.addClient(creator, playerName);
        lobbies.put(newLobbyId, lobby);

        // oyuncu listesi oluştur lobide göstermek için
        List<String> players = new ArrayList<>();
        players.add(playerName);

        sendLobbyUpdate(lobby);

        // server mesajı oluştur
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.setType(ServerResponseType.lobbyCreated);
        serverMessage.setLobbyId(newLobbyId);
        serverMessage.setMessage("waiting to start");
        serverMessage.setPlayers(players);

        try {
            String message = objectMapper.writeValueAsString(serverMessage);
            creator.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void joinLobby(String lobbyId, ClientHandler client, String playerName) {
        Game lobby = lobbies.get(lobbyId);
        String message = null;
        if (lobby != null) {
            if (lobby.getClients().size() < 3) {
                lobby.addClient(client, playerName);
                sendlobbyJoinState(client,null);
                sendLobbyUpdate(lobby);
            }
            else {
                message = "Lobby is full";
                sendlobbyJoinState(client, message);
            }
        }
        else {
            message = "Lobby not found";
            sendlobbyJoinState(client, message);
        }

    }

    private void sendlobbyJoinState(ClientHandler client, String message) {
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.setType(ServerResponseType.lobbyJoinState);
        serverMessage.setMessage(message);
        try {
            client.sendMessage(objectMapper.writeValueAsString(serverMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLobbiesList(ClientHandler client) {
        if (!lobbies.isEmpty()) {
            for (Game lobby : lobbies.values()) {
                if (lobby.isGameStateTypeGameOver() || (lobby.getClients().isEmpty())) {
                    lobbies.remove(lobby.getLobbyId());
                }
            }
        }

        List<String> availableLobbies = new ArrayList<>(lobbies.keySet());
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.setType(ServerResponseType.lobbiesList);
        serverMessage.setLobbies(availableLobbies);

        try {
            String message = objectMapper.writeValueAsString(serverMessage);
            client.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendLobbyUpdate(Game lobby) {
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.setType(ServerResponseType.lobbyUpdate);
        List<String> players = new ArrayList<>();
        for (ClientHandler client : lobby.getClients()) {
            String playerName = client.getPlayerName();
            if (playerName != null) {
                players.add(playerName);
            }
        }
        serverMessage.setPlayers(players);
        serverMessage.setLobbyId(lobby.getLobbyId());
        String message = "player added:"+ players.get(players.size() - 1);
        serverMessage.setMessage(message);
        try {
            updateGames();
            broadcast(objectMapper.writeValueAsString(serverMessage), lobby.getLobbyId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void updateGames() {
        for (Game lobby : lobbies.values()) {
            lobby.update();
        }
    }
}
