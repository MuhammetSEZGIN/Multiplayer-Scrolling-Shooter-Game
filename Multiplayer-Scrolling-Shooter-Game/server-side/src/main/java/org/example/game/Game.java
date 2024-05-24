package org.example.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.server.ClientHandler;
import org.example.server.ClientMessage;
import org.example.server.GameServer;
import org.example.server.ServerMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.SQLOutput;
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
    private boolean gameStarted;
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
        gameStarted = false;
    }

    public void addClient(ClientHandler client, String playerName) {
        if (clients.size() < 3) {
            clients.add(client);
            playerNames.put(playerName.hashCode(), client);
            ships.add(new Ship(playerName.hashCode(), 100, 100, 4)); // Örnek gemi
        } else {
            client.sendMessage("Lobby is full");

        }
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public void processMessage(ClientMessage clientMessage, ClientHandler sender) {
        switch (clientMessage.getType()) {
            case "move":
                handleMove(clientMessage, sender);
                break;
            case "shoot":
                handleShoot(clientMessage, sender);
                break;
            case "startGame":
                startGame();
                break;
            case "chat":
                System.out.printf("Chat message: \n gönderilecek:" + clientMessage.getMessage() + " \nLobi id:" + clientMessage.getLobbyId() + "\n");
                server.sendChatMassage(clientMessage.getPlayerName(),clientMessage.getMessage(), clientMessage.getLobbyId());                break;
            case "joinLobby":

                System.out.println(clientMessage.getPlayerName()+" "+clientMessage.getLobbyId());
                server.joinLobby(clientMessage.getLobbyId(), sender, clientMessage.getPlayerName());
                System.out.println("Joining lobby");
                break;
        }
    }

    private void handleMove(ClientMessage clientMessage, ClientHandler sender) {
        Ship ship = findShipByClient(sender);
        // System.out.println("Ship info"+ship);
        if (ship != null) {
            ship.setX(clientMessage.getX());
            ship.setY(clientMessage.getY());
            // System.out.println("Updated Ship Position: " + ship.getX() + ", " + ship.getY()); // Hata ayıklama için ekledik
            sendGameStateToClients(); // Oyun durumunu tüm istemcilere gönder
        }
    }

    private void handleShoot(ClientMessage clientMessage, ClientHandler sender) {
        Ship ship = findShipByClient(sender);
        if (ship != null) {
            Bullet bullet = new Bullet(ship.getX(), ship.getY(), 10, sender); // Mermiyi geminin pozisyonundan başlat
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
        if (gameStarted) {
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
    // düşmanların rastgele oluşturulması
    private void addRandomEnemy() {
        Random rand = new Random();
        int x, y;
        boolean isColliding;

        do {
            x = rand.nextInt(800 - 50); // assuming enemy width is 50
            y = 0; // assuming enemy height is 50
            isColliding = false;

            for (Enemy enemy : enemies) {
                if (isCollidingWithExistingEnemies(x, y, enemy)) {
                    isColliding = true;
                    break;
                }
            }
        } while (isColliding);

        enemies.add(new Enemy(1, x, y, 2, 3));
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
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            boolean hit = false;

            // Mermi-düşman çarpışması
            Iterator<Enemy> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();
                if (checkCollision(bullet, enemy)) {
                    hit = true;
                    enemy.decreaseHealth(); // Düşmana hasar ver
                    if (enemy.getHealth() <= 0) {
                        enemyIterator.remove(); // Düşmanı kaldır
                        if(bullet.getOwner() != null){
                            findShipByClient(bullet.getOwner()).increaseScore(); // Vuran gemiye puan ekle
                        }
                    }
                    break;
                }
            }

            if (hit) {
                bulletIterator.remove(); // Mermiyi kaldır
            }
        }

        // Gemi-düşman çarpışması
        Iterator<Ship> shipIterator = ships.iterator();
        while (shipIterator.hasNext()) {
            Ship ship = shipIterator.next();
            Iterator<Enemy> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();
                if (checkCollision(ship, enemy)) {
                    ship.decreaseHealth(); // Gemiye hasar ver
                    enemy.decreaseHealth(); // Düşmana hasar ver

                    // Eğer düşmanın sağlığı sıfır veya daha az ise düşmanı kaldır
                    // Eğer geminin sağlığı sıfır veya daha az ise gemiyi kaldır
                    if (ship.getHealth() <= 0) {
                        rankings.put(playerNames.get(ship.getId()).getPlayerName(), ship.getScore());
                        shipIterator.remove();
                        enemyIterator.remove();
                        break; // İç içe döngüden çıkmak için
                    }
                }
            }
        }
    }

    private void checkGameOver() {
        if (ships.isEmpty()) {
            // Oyun bitti, kazananı belirle
            sendGameOverInfo();

        }
    }


//
//     private Ship determineWinner() {
//        return ships.stream().max(Comparator.comparingInt(Ship::getScore)).orElse(null);
//    }

    private boolean checkCollision(GameObject obj1, GameObject obj2) {
        return obj1.getX() < obj2.getX() + obj2.getWidth() &&
                obj1.getX() + obj2.getWidth() > obj2.getX() &&
                obj1.getY() < obj2.getY() + obj2.getHeight() &&
                obj1.getY() + obj2.getHeight() > obj2.getY();
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
        gameState.setType("gameOver");
        try {
            ServerMessage serverMessage = new ServerMessage();
            serverMessage.setGameState(gameState);
            serverMessage.setType("gameState");
            serverMessage.setPlayers(PlayerRanking);
            String message = objectMapper.writeValueAsString(serverMessage);
            gameStarted = false;
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
        if (gameStarted) {
            gameState.setType("GameGoingOn");
        } else {
            gameState.setType("GameOver");
        }
        try {
            String gameStateJson = objectMapper.writeValueAsString(gameState);
            ServerMessage serverMessage = new ServerMessage();
            serverMessage.setType("gameState");
            serverMessage.setGameState(gameState);
            String message = objectMapper.writeValueAsString(serverMessage);
            server.broadcast(message, lobbyId);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startGame() {
        gameStarted = true;
    }
}
