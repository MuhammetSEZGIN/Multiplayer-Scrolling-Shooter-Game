package org.example.game;

import org.example.server.ClientHandler;
import org.example.server.ClientMessage;
import org.example.server.GameServer;
import org.example.server.ServerMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Game {
    private List<Ship> ships;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private List<ClientHandler> clients;
    private Map<ClientHandler, String> playerNames;
    private GameServer server;
    private String lobbyId;
    private ObjectMapper objectMapper;
    private boolean gameStarted;
    private long spawnInterval;
    private long lastEnemySpawnTime;

    public Game(GameServer server, String lobbyId) {
        this.server = server;
        this.lobbyId = lobbyId;
        ships = new ArrayList<>();
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        clients = new ArrayList<>();
        playerNames = new ConcurrentHashMap<>();
        objectMapper = new ObjectMapper();
        this.lastEnemySpawnTime = System.currentTimeMillis();
        this.spawnInterval = 500; // Örnek düşman
        gameStarted = false;
    }

    public void addClient(ClientHandler client, String playerName) {
        if (clients.size() < 3) {
            clients.add(client);
            playerNames.put(client, playerName);
            ships.add(new Ship(client.hashCode(), 100, 100, 3)); // Örnek gemi
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
            case "joinLobby":

                System.out.println(clientMessage.getPlayerName()+" "+clientMessage.getLobbyId());
                server.joinLobby(clientMessage.getLobbyId(), sender, clientMessage.getPlayerName());
                System.out.println("Joining lobby");
                break;
        }
    }

    private void handleMove(ClientMessage clientMessage, ClientHandler sender) {
        Ship ship = findShipByClient(sender);
        System.out.println("Ship info"+ship);
        if (ship != null) {
            ship.setX(clientMessage.getX());
            ship.setY(clientMessage.getY());
            System.out.println("Updated Ship Position: " + ship.getX() + ", " + ship.getY()); // Hata ayıklama için ekledik
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
        return ships.stream().filter(ship -> ship.getId() == client.hashCode()).findFirst().orElse(null);
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
                        findShipByClient(bullet.getOwner()).increaseScore(); // Vuran gemiye puan ekle
                    }
                    break;
                }
            }

            if (hit) {
                bulletIterator.remove(); // Mermiyi kaldır
            }
        }

        // Gemi-düşman çarpışması
        for (Ship ship : ships) {
            for (Enemy enemy : enemies) {
                if (checkCollision(ship, enemy)) {
                    ship.decreaseHealth(); // Gemiye hasar ver
                    if (ship.getHealth() <= 0) {
                        removeShip(ship);
                    }
                }
            }
        }
    }

    private void checkGameOver() {
        if (ships.isEmpty()) {
            // Oyun bitti, kazananı belirle
            Ship winner = determineWinner();
            if (winner != null) {
                server.broadcast("Game Over. Winner: " + winner.getId() + " with score: " + winner.getScore(), lobbyId);
            } else {
                server.broadcast("Game Over. No winner.", lobbyId);
            }
        }
    }

    private Ship determineWinner() {
        return ships.stream().max(Comparator.comparingInt(Ship::getScore)).orElse(null);
    }

    private boolean checkCollision(GameObject obj1, GameObject obj2) {
        return obj1.getX() < obj2.getX() + obj2.getWidth() &&
                obj1.getX() + obj2.getWidth() > obj2.getX() &&
                obj1.getY() < obj2.getY() + obj2.getHeight() &&
                obj1.getY() + obj2.getHeight() > obj2.getY();
    }

    private void removeShip(Ship ship) {
        ships.remove(ship);
        server.broadcast("Ship destroyed: " + ship.getId(), lobbyId);
    }

    private void sendGameStateToClients() {
        GameState gameState = new GameState();
        gameState.setShips(ships);
        gameState.setEnemies(enemies);
        gameState.setBullets(bullets);

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
