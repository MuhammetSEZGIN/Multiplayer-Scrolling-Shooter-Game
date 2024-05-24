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
    private TextField chatField;
    private Canvas gameCanvas;
    private double shipX = 400;
    private double shipY = 300;
    private static final double SHIP_SPEED = 5;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    private Label healthLabel;
    private Label scoreLabel;
    private Boolean gameGoingOn;
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
            createLobby(primaryStage);
        });

        root.getChildren().addAll(joinLobbyButton, createLobbyButton);

        primaryStage.setScene(scene);
    }

    private void showAvailableLobbies(Stage primaryStage) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        ListView<String> lobbiesListView = new ListView<>();
        Button refreshButton = new Button("Refresh");
//        lobbiesTextArea = new TextArea();
//        lobbiesTextArea.setEditable(false);

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
            Platform.runLater(() -> {
                System.out.println("Updating lobby list: " + lobbies); // Debugging line
                lobbiesListView.getItems().setAll(lobbies);

            });
        });



        root.getChildren().addAll(lobbiesListView, refreshButton, joinButton);

        primaryStage.setScene(scene);

        // İlk başta lobileri göster
        refreshButton.fire();
    }

    private void joinLobby(String lobbyId, Stage primaryStage) {
        // gameClient = new GameClient(playerName, lobbyId);
        gameClient.setLobbyId(lobbyId);
        gameClient.start();

        showLobbyScreen(primaryStage, gameClient);
    }

    private void createLobby(Stage primaryStage) {
      //
        gameClient.createLobby();
        lobbyId = gameClient.getLobbyId();
        System.out.println("Created lobby with ID: " + lobbyId);
        showLobbyScreen(primaryStage, gameClient);
    }

    private void showLobbyScreen(Stage primaryStage, GameClient gameClient) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        lobbyPlayersTextArea = new TextArea();
        lobbyPlayersTextArea.setEditable(false);

        chatField = new TextField();
        chatField.setPromptText("Enter chat message");

        Button startGameButton = new Button("Start Game");
        lobbyId = gameClient.getLobbyId();

        gameClient.setLobbyCreatedCallback(lobbyId -> {
            Platform.runLater(() -> {
                root.getChildren().add(new Label("Lobby: " + lobbyId));
            });
        });


        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> {

            String message = chatField.getText();
            System.out.println("Message: " + message);
            if(!(message == null || message.isEmpty())) {
                gameClient.sendChatMessage(message);
                chatField.clear();
            }
        });


        gameClient.setLobbyUpdateCallback(players -> {
            if (players != null) {
                Platform.runLater(() -> {
                    lobbyPlayersTextArea.clear();
                    for (String player : players) {
                        if (player != null) {
                            lobbyPlayersTextArea.appendText(player + " has joined the lobby\n");
                            lobbyId = gameClient.getLobbyId();
                            System.out.println("Lobby ID: " + lobbyId);
                        }
                    }
                });
            }
        });

        gameClient.setChatCallback(message -> {
            if (message != null) {
                Platform.runLater(() -> {
                    lobbyPlayersTextArea.appendText(  message + "\n");
                });
            }
        });

        root.getChildren().addAll(lobbyPlayersTextArea, chatField,sendButton, startGameButton);
        startGameButton.setOnAction(event -> startGame(primaryStage));

        primaryStage.setScene(scene);
    }

    private void startGame(Stage primaryStage) {
        gameClient.startGame();
        showGameScreen(primaryStage);
    }

    private void showGameScreen(Stage primaryStage) {
        VBox root = new VBox(10);
        Pane gamePane = new Pane();
        gameCanvas = new Canvas(800, 600);
        gamePane.getChildren().add(gameCanvas);

        healthLabel = new Label("Health: ");
        scoreLabel = new Label("Score: ");
        root.getChildren().addAll(healthLabel, scoreLabel, gamePane);

        Scene scene = new Scene(root, 800, 600);

        gameClient.setGameCanvas(gameCanvas);
        gameGoingOn = true;

        gameClient.setGameUpdateCallback(gameState -> {
            Platform.runLater(() -> {
                if(gameState.getType().equals("GameGoingOn")) {
                    drawGameState(gameState);
                    updatePlayerStats(gameState);
                }
                else{
                    gameGoingOn = false;
                    gameOverScreen(primaryStage);

                }
            });
        });

        scene.setOnKeyPressed(this::handleKeyPressed);
        scene.setOnKeyReleased(this::handleKeyReleased);
        if(gameGoingOn){
            startGameLoop();
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void gameOverScreen(Stage primaryStage) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        Label gameOverLabel = new Label("Game Over!");
        TextArea scoresTextArea = new TextArea();
        scoresTextArea.setEditable(false);
        Button returnToLobbyButton = new Button("Return to Lobby");

        gameClient.setGameOverCallback(players -> {
            Platform.runLater(() -> {
                for (String player : players) {
                    scoresTextArea.appendText(player + "\n");
                }
            });
        });
        returnToLobbyButton.setOnAction(event -> {
            showLobbyOptions(primaryStage);
        });

        root.getChildren().addAll(gameOverLabel, returnToLobbyButton);

        primaryStage.setScene(scene);

    }

    private void updatePlayerStats(GameState gameState) {
        for (Ship ship : gameState.getShips()) {
            if (ship.getId() == playerName.hashCode()) {
                healthLabel.setText("Health: " + ship.getHealth());
                scoreLabel.setText("Score: " + ship.getScore());
                System.out.println("Health: " + ship.getHealth() + ", Score: " + ship.getScore());
                break;
            }
        }
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
