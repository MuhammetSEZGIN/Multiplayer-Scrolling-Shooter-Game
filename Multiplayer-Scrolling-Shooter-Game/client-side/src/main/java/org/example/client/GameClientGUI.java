package org.example.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.animation.AnimationTimer;
import org.example.game.Bullet;
import org.example.game.Enemy;
import org.example.game.GameState;
import org.example.game.Ship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameClientGUI extends Application {
    private GameClient gameClient;
    private String playerName;
    private String lobbyId;
    private TextArea lobbyPlayersTextArea;
    private Canvas gameCanvas;
    private double shipX = 400;
    private double shipY = 300;
    private static final double SHIP_SPEED = 5;
    private Set<KeyCode> pressedKeys = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Multiplayer Shooter Game");

        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        // Oyuncu adını girme ekranı
        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(event -> {
            playerName = nameField.getText();
            gameClient = new GameClient(playerName);
            gameClient.start();
            showLobbyOptions(primaryStage);
        });

        root.getChildren().addAll(nameField, submitButton);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showLobbyOptions(Stage primaryStage) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        Button joinLobbyButton = new Button("Join Lobby");
        joinLobbyButton.setOnAction(event -> {
            // GameClient nesnesini başlat
            // Bağlantıyı başlat
            showAvailableLobbies(primaryStage);
        });

        Button createLobbyButton = new Button("Create Lobby");
        createLobbyButton.setOnAction(event -> {
            gameClient.createLobby(); // Lobi oluşturma işlemini başlat
            lobbyId = gameClient.getLobbyId(); // Oluşturulan lobi id'sini al
            showLobbyScreen(primaryStage);
        });

        root.getChildren().addAll(joinLobbyButton, createLobbyButton);

        primaryStage.setScene(scene);
    }

    private void showAvailableLobbies(Stage primaryStage) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        ListView<String> lobbiesListView = new ListView<>();
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> {
            gameClient.requestAvailableLobbies();

        });

        Button joinButton = new Button("Join");
        joinButton.setOnAction(event -> {
            lobbyId = lobbiesListView.getSelectionModel().getSelectedItem();
            if (lobbyId != null) {
                joinLobby(lobbyId, primaryStage);
            }
        });

        gameClient.setLobbyUpdateCallback(lobbies -> {
            Platform.runLater(() -> lobbiesListView.getItems().setAll(lobbies));
        });

        root.getChildren().addAll(lobbiesListView, refreshButton, joinButton);

        primaryStage.setScene(scene);

        // İlk başta lobileri göster
        refreshButton.fire();
    }

    private void joinLobby(String lobbyId, Stage primaryStage) {
        gameClient = new GameClient(playerName, lobbyId);
        gameClient.start();

        showLobbyScreen(primaryStage);
    }

    private void createLobby(Stage primaryStage) {
        gameClient = new GameClient(playerName);
        gameClient.createLobby();
        lobbyId = gameClient.getLobbyId();

        showLobbyScreen(primaryStage);
    }

    private void showLobbyScreen(Stage primaryStage) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        lobbyPlayersTextArea = new TextArea();
        lobbyPlayersTextArea.setEditable(false);

        Button startGameButton = new Button("Start Game");
        startGameButton.setOnAction(event -> startGame(primaryStage));

        root.getChildren().addAll(new Label("Lobby: " + lobbyId), lobbyPlayersTextArea, startGameButton);

        primaryStage.setScene(scene);

        gameClient.setLobbyUpdateCallback(players -> {
            if (players != null) {
                Platform.runLater(() -> {
                    lobbyPlayersTextArea.clear();
                    for (String player : players) {
                        if (player != null) {
                            lobbyPlayersTextArea.appendText(player + "\n");
                        }
                    }
                });
            }
        });
    }

    private void startGame(Stage primaryStage) {
        gameClient.startGame();
        showGameScreen(primaryStage);
    }

    private void showGameScreen(Stage primaryStage) {
        gameCanvas = new Canvas(800, 600);
        Pane root = new Pane(gameCanvas);
        Scene scene = new Scene(root, 800, 600);

        gameClient.setGameCanvas(gameCanvas);

        gameClient.setGameUpdateCallback(gameState -> {
            System.out.println("Received game state: " + gameState);
            System.out.println("Ships: " + gameState.getShips());
            System.out.println("Enemies: " + gameState.getEnemies());
            System.out.println("Bullets: " + gameState.getBullets());
            Platform.runLater(() -> {
                drawGameState(gameState);
            });
        });

        // Klavye olaylarını dinleme
        scene.setOnKeyPressed(this::handleKeyPressed);
        scene.setOnKeyReleased(this::handleKeyReleased);

        // Start the game loop for continuous movement
        startGameLoop();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleKeyPressed(KeyEvent event) {
        pressedKeys.add(event.getCode());
        if (event.getCode() == KeyCode.SPACE) {
            gameClient.sendMessage(new ClientMessage("shoot", gameClient.getLobbyId(), shipX, shipY, playerName));
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        pressedKeys.remove(event.getCode());
    }

    private void startGameLoop() {
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (pressedKeys.contains(KeyCode.UP)) {
                    shipY -= SHIP_SPEED;
                }
                if (pressedKeys.contains(KeyCode.DOWN)) {
                    shipY += SHIP_SPEED;
                }
                if (pressedKeys.contains(KeyCode.LEFT)) {
                    shipX -= SHIP_SPEED;
                }
                if (pressedKeys.contains(KeyCode.RIGHT)) {
                    shipX += SHIP_SPEED;
                }

                if (!pressedKeys.isEmpty()) {
                    gameClient.sendMessage(new ClientMessage("move", gameClient.getLobbyId(), shipX, shipY, playerName));
                    System.out.println("Sent move message with coordinates: (" + shipX + ", " + shipY + ")");
                }
            }
        };
        gameLoop.start();
    }

    private void drawGameState(GameState gameState) {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Düşmanları çiz
        for (Enemy enemy : gameState.getEnemies()) {
            gc.setFill(Color.RED);
            gc.fillRect(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());
        }

        // Gemileri çiz
        for (Ship ship : gameState.getShips()) {
            System.out.println("Drawing Ship at: " + ship.getX() + ", " + ship.getY()); // Hata ayıklama için ekledik
            gc.setFill(Color.BLUE);
            gc.fillRect(ship.getX(), ship.getY(), ship.getWidth(), ship.getHeight());
        }

        // Mermileri çiz
        for (Bullet bullet : gameState.getBullets()) {
            gc.setFill(Color.YELLOW);
            gc.fillRect(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
