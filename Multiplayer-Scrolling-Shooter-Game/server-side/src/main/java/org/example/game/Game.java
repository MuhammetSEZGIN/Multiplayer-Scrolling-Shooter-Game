package org.example.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.server.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Game {
    private List<Ship> ships;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private List<ClientHandler> clients;
    private Map<Integer, ClientHandler> playerNames;
    private GameServer server;
    private String lobbyId;
    private ObjectMapper objectMapper;
    private GameStateType gameStateType;
    private long spawnInterval;
    private long lastEnemySpawnTime;
    private Map<String, Integer> rankings;

    public Game(GameServer server, String lobbyId) {
        this.server = server;
        this.lobbyId = lobbyId;
        ships = new ArrayList<>();
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        clients = new ArrayList<>();
        rankings = new HashMap<>();
        playerNames = new ConcurrentHashMap<>();
        objectMapper = new ObjectMapper();
        this.lastEnemySpawnTime = System.currentTimeMillis();
        this.spawnInterval = 1000; // düşman oluşturma aralığı
        gameStateType = GameStateType.GameNotStart;
    }

    public void addClient(ClientHandler client, String playerName) {
        clients.add(client);
        playerNames.put(playerName.hashCode(), client);
        ships.add(new Ship(playerName.hashCode(), 400, 300, 4));
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public void processMessage(ClientMessage clientMessage, ClientHandler sender) {
        switch (clientMessage.getType()) {
            case move:
                handleMove(clientMessage, sender);
                break;
            case shoot:
                handleShoot(clientMessage, sender);
                break;
            case startGame:
                startGame();
                break;
            case chat:
                System.out.printf("Chat message: \n gönderilecek:" + clientMessage.getMessage() + " \nLobi id:" + clientMessage.getLobbyId() + "\n");
                server.sendChatMassage(clientMessage.getPlayerName(),clientMessage.getMessage(), clientMessage.getLobbyId());
                break;
        }
    }

    private void handleMove(ClientMessage clientMessage, ClientHandler sender) {
        Ship ship = findShipByClient(sender);
        if (ship != null) {
            ship.setX(clientMessage.getX());
            ship.setY(clientMessage.getY());
            sendGameStateToClients(); // Oyun durumunu tüm istemcilere gönder
        }
    }

    private void handleShoot(ClientMessage clientMessage, ClientHandler sender) {
        Ship ship = findShipByClient(sender);
        if (ship != null) {
            Bullet bullet = new Bullet(ship.getX(), ship.getY(), 5, sender);
            addBullet(bullet);
        }
    }

    private Ship findShipByClient(ClientHandler client) {
        return ships.stream().filter(ship -> ship.getId() == client.getPlayerName().hashCode()).findFirst().orElse(null);
    }

    private void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public void update() {
        if (gameStateType.equals(GameStateType.GameGoingOn)) {
            updateShips();
            updateEnemies();
            updateBullets();
            checkCollisions();
            checkGameOver();
            sendGameStateToClients();
        }
    }

    private void updateShips() {
        for (Ship ship : ships) {
            ship.update();
        }
    }

    private void updateEnemies() {
        long currentTime = System.currentTimeMillis();
        if(currentTime - lastEnemySpawnTime >= spawnInterval) {
            addRandomEnemy();
            lastEnemySpawnTime = currentTime;
        }

        for (Enemy enemy : enemies) {
            enemy.update();
        }
    }

    private void addRandomEnemy() {
        Random rand = new Random();
        int x, y;
        boolean isColliding;

        do {
            x = rand.nextInt(800 - 50);
            y = 0; // assuming enemy height is 50
            isColliding = false;

            for (Enemy enemy : enemies) {
                if (isCollidingWithExistingEnemies(x, y, enemy)) {
                    isColliding = true;
                    break;
                }
            }
        } while (isColliding);

        enemies.add(new Enemy(1, x, y, 1, 3));
    }
    // oluşturulan düşmanların aynı yerde oluşmaması için kontrol
    private boolean isCollidingWithExistingEnemies(int x, int y, Enemy existingEnemy) {
        int enemyWidth = 50; // example enemy width
        int enemyHeight = 50; // example enemy height

        return (x < existingEnemy.getX() + enemyWidth && x + enemyWidth > existingEnemy.getX() &&
                y < existingEnemy.getY() + enemyHeight && y + enemyHeight > existingEnemy.getY());
    }

    private void updateBullets() {
        Iterator<Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.update();
            // Mermi ekran dışına çıktıysa kaldır
            if (bullet.getY() < 0) {
                iterator.remove();
            }
        }
    }

    private void checkCollisions() {
        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<Ship> shipsToRemove = new ArrayList<>();

        // Mermi-düşman çarpışması
        for (Bullet bullet : bullets) {
            boolean hit = false;
            for (Enemy enemy : enemies) {
                if (checkCollision(bullet, enemy)) {
                    hit = true;
                    enemy.decreaseHealth(); // Düşmana hasar ver
                    if (enemy.getHealth() <= 0) {
                        enemiesToRemove.add(enemy); // Düşmanı kaldır
                    }
                    if (bullet.getOwner() != null) {
                        findShipByClient(bullet.getOwner()).increaseScore(); // Vuran gemiye puan ekle
                    }
                    break; // Aynı mermi ile birden fazla düşmana çarpma durumunu önlemek için
                }
            }
            if (hit) {
                bulletsToRemove.add(bullet); // Mermiyi kaldır
            }
        }

        // Listeleri güncelle
        bullets.removeAll(bulletsToRemove);
        enemies.removeAll(enemiesToRemove);

        // Gemi-düşman çarpışması
        for (Ship ship : ships) {
            for (Enemy enemy : enemies) {
                if (checkCollision(ship, enemy)) {
                    ship.decreaseHealth(); // Gemiye hasar ver
                    enemy.decreaseHealth(); // Düşmana hasar ver

                    if (ship.getHealth() <= 0) {
                        rankings.put(playerNames.get(ship.getId()).getPlayerName(), ship.getScore());
                        shipsToRemove.add(ship); // Gemiyi kaldır
                    }
                    if (enemy.getHealth() <= 0) {
                        enemiesToRemove.add(enemy); // Düşmanı kaldır
                    }
                }
            }
        }

        // Listeleri güncelle
        ships.removeAll(shipsToRemove);
        enemies.removeAll(enemiesToRemove);
    }


    private void checkGameOver() {
        if (ships.isEmpty()) {
            // Oyun bitti, kazananı belirle
            sendGameOverInfo();
            clearGame();
        }
    }

    private void clearGame(){
        this.enemies.clear();
        this.clients.clear();
        this.bullets.clear();
        this.playerNames.clear();
        this.gameStateType =GameStateType.GameNotStart;
        this.ships.clear();

    }

    private boolean checkCollision(GameObject obj1, GameObject obj2) {
        return obj1.getX() < obj2.getX() + obj2.getWidth() &&
                obj1.getX() + obj1.getWidth() > obj2.getX() &&
                obj1.getY() < obj2.getY() + obj2.getHeight() &&
                obj1.getY() + obj1.getHeight() > obj2.getY();
    }


    private void sendGameOverInfo(){
        Map<String, Integer> sortedRankings = sortByValue(rankings);
        List<String> PlayerRanking = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sortedRankings.entrySet()) {
            PlayerRanking.add(entry.getKey()+" - Score:" +entry.getValue());
            System.out.println("Player: " + entry.getKey() + " Score: " + entry.getValue());
        }

        GameState gameState = new GameState();
        gameState.setShips(ships);
        gameState.setEnemies(enemies);
        gameState.setBullets(bullets);
        gameState.setGameScores(PlayerRanking);
        gameState.setType(GameStateType.GameOver);
        try {
            ServerMessage serverMessage = new ServerMessage();
            serverMessage.setGameState(gameState);
            serverMessage.setType(ServerResponseType.gameState);
            serverMessage.setPlayers(PlayerRanking);
            String message = objectMapper.writeValueAsString(serverMessage);
            gameStateType = GameStateType.GameOver;
            server.broadcast(message, lobbyId);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


    }

    public static Map<String, Integer> sortByValue(Map<String, Integer> unsortedMap) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(unsortedMap.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private void sendGameStateToClients() {
        GameState gameState = new GameState();
        gameState.setShips(ships);
        gameState.setEnemies(enemies);
        gameState.setBullets(bullets);
        if (gameStateType.equals(GameStateType.GameGoingOn)) {
            gameState.setType(GameStateType.GameGoingOn);
        } else {
            gameState.setType(GameStateType.GameOver);
        }
        try {

            ServerMessage serverMessage = new ServerMessage();
            serverMessage.setType(ServerResponseType.gameState);
            serverMessage.setGameState(gameState);
            String message = objectMapper.writeValueAsString(serverMessage);
            server.broadcast(message, lobbyId);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startGame() {
        gameStateType = GameStateType.GameGoingOn;
        sendGameStateToClients();
    }

    public boolean isGameStateTypeGameOver() {
        return gameStateType.equals(GameStateType.GameOver);
    }

}
