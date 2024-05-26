package org.example.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.animation.AnimationTimer;
import org.example.game.*;

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
    private static final double SHIP_SPEED = 300;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    private Label healthLabel;
    private Label scoreLabel;
    private GameStateType gameStateType;
    private AnimationTimer gameLoop;
    private Image airplaneImage;
    private Image kamikazeImage;
    private Image bulletImage;
    private Image backgroundImage;

    @Override
    public void start(Stage primaryStage) {

        airplaneImage = new Image(getClass().getResource("/images/ship.png").toExternalForm());
        kamikazeImage = new Image(getClass().getResource("/images/kamikaze.png").toExternalForm());
        bulletImage = new Image(getClass().getResource("/images/bullet.png").toExternalForm());
        backgroundImage = new Image(getClass().getResource("/images/background1.png").toExternalForm());

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
            // Bağlantıyı başlat
            showAvailableLobbies(primaryStage);
        });

        Button createLobbyButton = new Button("Create Lobby");
        createLobbyButton.setOnAction(event -> createLobby(primaryStage));

        root.getChildren().addAll(joinLobbyButton, createLobbyButton);

        primaryStage.setScene(scene);
    }

    private void showAvailableLobbies(Stage primaryStage) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        ListView<String> lobbiesListView = new ListView<>();
        Button refreshButton = new Button("Refresh");

        Button returnTo = new Button("Back");
        returnTo.setOnAction(
                event-> showLobbyOptions(primaryStage)
        );
        refreshButton.setOnAction(event -> gameClient.requestAvailableLobbies());

        Button joinButton = new Button("Join");
        joinButton.setOnAction(event -> {
            lobbyId = lobbiesListView.getSelectionModel().getSelectedItem();
            if (lobbyId != null) {
                joinLobby(lobbyId, primaryStage);
            }
        });

        gameClient.setLobbyUpdateCallback(lobbies -> Platform.runLater(() -> {
            System.out.println("Updating lobby list: " + lobbies); // Debugging line
            lobbiesListView.getItems().setAll(lobbies);

        }));

        root.getChildren().addAll(lobbiesListView, refreshButton, joinButton, returnTo);

        primaryStage.setScene(scene);

        // İlk başta lobileri göster
        refreshButton.fire();
    }


    private void joinLobby(String lobbyId, Stage primaryStage) {
        gameClient.sendJoinLobbyMessage(lobbyId);
        final boolean[] flag = new boolean[1];
        flag[0] = true;
        gameClient.setLobbyJoinCallback(message -> {
            if (message != null) {
                Platform.runLater(() -> {
                    if (message.equals("Lobby is full")){
                        showAlert(Alert.AlertType.WARNING,"Not joined" , message);
                    }
                    else if (message.equals("Lobby not found")){
                        showAlert(Alert.AlertType.WARNING,"Not joined" , message);
                    }
                });
                flag[0] = false;
            }
        });

        if (flag[0]) {
            showLobbyScreenFromJoin(primaryStage, gameClient);
        }

    }
    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // Optional: null means no header text
        alert.setContentText(message);
        alert.showAndWait(); // Show the alert and wait for user interaction
    }

    private void createLobby(Stage primaryStage) {

        gameClient.createLobby();
        lobbyId = gameClient.getLobbyId();
        System.out.println("Created lobby with ID: " + lobbyId);
        showLobbyScreenFromCreate(primaryStage, gameClient);
    }

    private void showLobbyScreenFromCreate(Stage primaryStage, GameClient gameClient) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        lobbyPlayersTextArea = new TextArea();
        lobbyPlayersTextArea.setEditable(false);

        chatField = new TextField();
        chatField.setPromptText("Enter chat message");

        Button startGameButton = new Button("Start Game");
        lobbyId = gameClient.getLobbyId();

        gameClient.setLobbyCreatedCallback(lobbyId -> Platform.runLater(() -> {
            root.getChildren().add(new Label("Lobby: " + lobbyId));
        }));


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

    private void showLobbyScreenFromJoin(Stage primaryStage, GameClient gameClient) {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        lobbyPlayersTextArea = new TextArea();
        lobbyPlayersTextArea.setEditable(false);

        chatField = new TextField();
        chatField.setPromptText("Enter chat message");

        lobbyId = gameClient.getLobbyId();

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

        gameClient.setGameUpdateCallback(gameState -> {
            if (gameState != null) {
                Platform.runLater(() -> {
                    if(gameState.getType().equals(GameStateType.GameGoingOn)){
                        showGameScreen(primaryStage);
                    }
                });
            }
        });

        root.getChildren().addAll(lobbyPlayersTextArea, chatField,sendButton);

        primaryStage.setScene(scene);
    }

    private void startGame(Stage primaryStage) {
        gameClient.startGame();
        pressedKeys.clear();
        showGameScreen(primaryStage);
    }

    private void showGameScreen(Stage primaryStage) {
        VBox root = new VBox(10);
        Pane gamePane = new Pane();
        gameCanvas = new Canvas(800, 600);
// Sağlık ve skor etiketlerini içeren VBox
        VBox labelBox = new VBox(10);
        labelBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0);"); // VBox'ı transparan yap
        labelBox.setLayoutX(10); // Etiketlerin x konumu
        labelBox.setLayoutY(10); // Etiketlerin y konumu

        healthLabel = new Label("Health: ");
        healthLabel.setStyle("-fx-text-fill: red; -fx-font-size: 20px; -fx-font-weight: bold;"); // Sağlık etiketi stilini ayarla

        scoreLabel = new Label("Score: ");
        scoreLabel.setStyle("-fx-text-fill: blue; -fx-font-size: 20px; -fx-font-weight: bold;"); // Skor etiketi stilini ayarla

        labelBox.getChildren().addAll(healthLabel, scoreLabel); // Etiketleri VBox'a ekle
        gamePane.getChildren().addAll(gameCanvas, labelBox); // Canvas ve VBox'ı gamePane'e ekle
        root.getChildren().add(gamePane); // gamePane'i root'a ekle

        Scene scene = new Scene(root, 800, 600);

        gameStateType = GameStateType.GameGoingOn;

        gameClient.setGameUpdateCallback(gameState -> {
            Platform.runLater(() -> {
                if (!gameState.getType().equals(GameStateType.GameOver)) {
                    drawGameState(gameState);
                    updatePlayerStats(gameState);
                    System.out.println("Game is going on: " + gameStateType);
                } else {
                    gameStateType = GameStateType.GameOver;
                    List<String> gameScores = gameState.getGameScores();
                    gameOverScreen(primaryStage, gameScores);
                }
            });
        });

        scene.setOnKeyPressed(this::handleKeyPressed);
        scene.setOnKeyReleased(this::handleKeyReleased);
        if (gameStateType.equals(GameStateType.GameGoingOn)) {
            startGameLoop();
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void gameOverScreen(Stage primaryStage,List<String> gameScores){
        VBox root = new VBox(10);
        Scene scene = new Scene(root, 300, 200);

        Label gameOverLabel = new Label("Game Over!");
        TextArea scoresTextArea = new TextArea();
        scoresTextArea.setEditable(false);
        Button returnToLobbyButton = new Button("Return to Lobby");

        if (gameScores != null){
            for (String scores : gameScores) {

                Platform.runLater(() -> {
                            System.out.println("Scores: " + scores);
                            scoresTextArea.appendText(scores + "\n");
                        }
                );
            }
        }


        returnToLobbyButton.setOnAction(event -> {
            showLobbyOptions(primaryStage);
        });

        root.getChildren().addAll(gameOverLabel,scoresTextArea, returnToLobbyButton);

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
            gameClient.sendMessage(new ClientMessage(ClientRequestType.shoot, gameClient.getLobbyId(), shipX, shipY, playerName));
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        pressedKeys.remove(event.getCode());
    }

    private void startGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop(); // Önceki döngüyü durdur
        }

        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // convert nanoseconds to seconds
                lastUpdate = now;

                double deltaX = 0;
                double deltaY = 0;

                if (pressedKeys.contains(KeyCode.UP)) {
                    deltaY -= SHIP_SPEED * deltaTime;
                }
                if (pressedKeys.contains(KeyCode.DOWN)) {
                    deltaY += SHIP_SPEED * deltaTime;
                }
                if (pressedKeys.contains(KeyCode.LEFT)) {
                    deltaX -= SHIP_SPEED * deltaTime;
                }
                if (pressedKeys.contains(KeyCode.RIGHT)) {
                    deltaX += SHIP_SPEED * deltaTime;
                }

                shipX += deltaX;
                shipY += deltaY;

                if (deltaX != 0 || deltaY != 0) {
                    gameClient.sendMessage(new ClientMessage(ClientRequestType.move, gameClient.getLobbyId(), shipX, shipY, playerName));
                }
            }
        };
        gameLoop.start();
    }



    private void drawGameState(GameState gameState) {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Arka planı çiz
        gc.drawImage(backgroundImage, 0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Düşmanları çiz
        for (Enemy enemy : gameState.getEnemies()) {
            gc.drawImage(kamikazeImage, enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());
        }

        // Gemileri çiz
        for (Ship ship : gameState.getShips()) {
            gc.drawImage(airplaneImage, ship.getX(), ship.getY(), ship.getWidth(), ship.getHeight());
        }

        // Mermileri çiz
        for (Bullet bullet : gameState.getBullets()) {
            gc.drawImage(bulletImage, bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
