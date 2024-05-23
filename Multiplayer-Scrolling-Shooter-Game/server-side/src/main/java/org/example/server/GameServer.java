package org.example.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.game.Game;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private static final int PORT = 12345;
    private ExecutorService pool;
    private List<ClientHandler> clients;
    private Map<String, Game> lobbies;
    private ObjectMapper objectMapper;
    private Timer gameUpdateTimer;

    public GameServer() {
        pool = Executors.newFixedThreadPool(10);
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
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
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

        String type = clientMessageJson.getString("type");
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

//        System.out.println("clientMessage: \n "+
//                 "LobbyId:"+ clientMessage.getLobbyId()+
//                "\n Player Name: "+ clientMessage.getPlayerName()
//                );
        if (clientMessage.getLobbyId() != null) {
            Game lobby = lobbies.get(clientMessage.getLobbyId());
            if (lobby != null) {
                System.out.println(clientMessage.getMessage());
                lobby.processMessage(clientMessage, sender);
            }
        } else if ("createLobby".equals(clientMessage.getType())) {
            createLobby(sender, clientMessage.getPlayerName());
        } else if ("joinLobby".equals(clientMessage.getType())) {
            System.out.println(clientMessage.getPlayerName()+" "+clientMessage.getLobbyId());
            joinLobby(clientMessage.getLobbyId(), sender, clientMessage.getPlayerName());
        }

         else if ("getLobbies".equals(clientMessage.getType())) {
            sendLobbiesList(sender);
        }
    }

    public void sendChatMassage(String playerName,String message, String lobbyId){
        Game lobby = lobbies.get(lobbyId);
        // server mesajı oluştur
        System.out.println("Chat message: "+ message);
        System.out.println("lobby: "+ lobby.getLobbyId());
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.setType("chat");
        serverMessage.setLobbyId(lobbyId);
        message= playerName +": "+ message;
        serverMessage.setState(message);
        String chatMessage = null;
        try {
            chatMessage = objectMapper.writeValueAsString(serverMessage);

            if (lobby != null) {
                synchronized (lobby.getClients()) {
                    for (ClientHandler client : lobby.getClients()) {
                        client.sendMessage(chatMessage);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private void createLobby(ClientHandler creator, String playerName) {
        String newLobbyId = UUID.randomUUID().toString();
        Game lobby = new Game(this, newLobbyId);
        lobby.addClient(creator, playerName);
        lobbies.put(newLobbyId, lobby);
        sendLobbyUpdate(lobby);

        // oyuncu listesi oluştur lobide göstermek için
        List<String> players = new ArrayList<>();
        players.add(playerName);

       // server mesajı oluştur
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.setType("lobbyCreated");
        serverMessage.setLobbyId(newLobbyId);
        serverMessage.setState("waiting to start");
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
        if (lobby != null) {
            if (lobby.getClients().size() < 3) {
                lobby.addClient(client, playerName);
                System.out.println("Lobby found");

                sendLobbyUpdate(lobby);
            } else {
                client.sendMessage("Lobby is full");
            }
        } else {
            client.sendMessage("Lobby not found");
        }
    }

    private void sendLobbiesList(ClientHandler client) {
        List<String> availableLobbies = new ArrayList<>(lobbies.keySet());
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.setType("lobbiesList");
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
        serverMessage.setType("lobbyUpdate");
        List<String> players = new ArrayList<>();
        for (ClientHandler client : lobby.getClients()) {
            String playerName = client.getPlayerName();
            if (playerName != null) {
                players.add(playerName);
            }
        }
        serverMessage.setPlayers(players);
        serverMessage.setLobbyId(lobby.getLobbyId());
        String state = "player added:"+ players.get(players.size() - 1);
        serverMessage.setState(state);
        try {
            String message = objectMapper.writeValueAsString(serverMessage);
            updateGames();
            broadcast(message, lobby.getLobbyId());
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
