package org.example.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
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
    private static final double SHIP_SPEED = 3;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    private Label healthLabel;
    private Label scoreLabel;
    private GameStateType gameStateType;
    private AnimationTimer gameLoop;
    private Image airplaneImage;
    private Image kamikazeImage;
    private Image bulletImage;
    private Image backgroundImage;
    private Image backgroundImageMainMenu;
    @Override
    public void start(Stage primaryStage) {
        // Load images (consider a better folder structure, e.g., "assets")
        airplaneImage = new Image(getClass().getResource("/images/ship.png").toExternalForm());
        kamikazeImage = new Image(getClass().getResource("/images/kamikaze.png").toExternalForm());
        bulletImage = new Image(getClass().getResource("/images/bullet.png").toExternalForm());
        backgroundImage = new Image(getClass().getResource("/images/background1.png").toExternalForm());
        backgroundImageMainMenu = new Image(getClass().getResource("/images/background2.png").toExternalForm());

        // Stage and scene setup
        primaryStage.setTitle("Multiplayer Shooter Game");
        primaryStage.getIcons().add(airplaneImage); // Set window icon

        // Main layout (StackPane for easier background placement)
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 800, 600);

        // Background image
        ImageView backgroundView = new ImageView(backgroundImageMainMenu);
        root.getChildren().add(backgroundView);

        // Form layout (VBox for better organization)
        VBox formBox = new VBox(20); // Add spacing between elements
        formBox.setPadding(new Insets(20));
        formBox.setAlignment(Pos.CENTER);
        formBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 10;"); // Semi-transparent background

        // Form title
        Label titleLabel = new Label("Multiplayer Shooter Game");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20)); // Daha büyük ve kalın font
        titleLabel.setTextFill(Color.web("#FFD700")); // Altın sarısı renk
        titleLabel.setStyle("-fx-effect: dropshadow( gaussian , rgba(0,0,0,0.75) , 4,0,0,1 );"); // Gölge efekti

        // Player name input
        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        nameField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);"); // Semi-transparent background

        // Submit button
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(event -> {
            playerName = nameField.getText();
            if (!playerName.isEmpty()) { // Check if player name is not empty
                gameClient = new GameClient(playerName);
                gameClient.start();
                showLobbyOptions(primaryStage); // Transition to lobby scene
            } else {
                // Handle empty name (e.g., show an error message)
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Please enter your name.");
                alert.showAndWait();
            }
        });

        Button infoButton = new Button("Info");
        infoButton.setOnAction(event -> showInfoPopup(primaryStage));

        formBox.getChildren().addAll(titleLabel, nameField, submitButton , infoButton);
        root.getChildren().add(formBox);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showInfoPopup(Stage primaryStage) {
        Stage infoStage = new Stage();
        infoStage.initModality(Modality.APPLICATION_MODAL); // Ana pencereyi bloke et
        infoStage.initOwner(primaryStage);
        infoStage.setTitle("Oyun Hakkında");

        StackPane infoRoot = new StackPane();
        Scene infoScene = new Scene(infoRoot, 500, 500); // Pop-up boyutları

        // Bilgi ekranı içeriği (VBox)
        VBox infoBox = new VBox(20);
        infoBox.setPadding(new Insets(20));
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 10;");

        Label infoTitle = new Label("Oyun Hakkında");
        infoTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16)); // Başlık boyutu küçültüldü
        infoTitle.setTextFill(Color.WHITE);

        // Oyunun amacı ve kontrolleri
        Label gameObjectiveLabel = new Label("Oyunun Amacı:\n3 oyunculu bu 2D arcade oyununda amacınız, bir araya geldiğiniz lobi ortamında düşman gemileriyle savaşarak en yüksek puanı toplamak ve galip gelmektir. Klavye kontrolleriyle geminizi ustalıkla yönlendirin, düşmanları alt edin ve liderlik tablosunda zirveye çıkın!");
        gameObjectiveLabel.setFont(Font.font(14)); // Yazı boyutu küçültüldü
        gameObjectiveLabel.setTextFill(Color.WHITE);
        gameObjectiveLabel.setWrapText(true);

        Label gameControlLabel = new Label("Oyun Kontrolleri:\nHareket:\nW,A,S,D veya Yön Tuşları\nAteş Etme:\nSpace (Boşluk Tuşu)");
        gameControlLabel.setFont(Font.font(14)); // Yazı boyutu küçültüldü
        gameControlLabel.setTextFill(Color.WHITE);
        gameControlLabel.setWrapText(true);

        infoBox.getChildren().addAll(infoTitle, gameObjectiveLabel, gameControlLabel);
        infoRoot.getChildren().add(infoBox);

        infoStage.setScene(infoScene);
        infoStage.show(); // Pop-up'ı göster
    }

    private void showLobbyOptions(Stage primaryStage) {
        StackPane root = new StackPane(); // Esnek yerleşim için StackPane kullanımı
        Scene scene = new Scene(root, 800, 600); // Pencere boyutunu büyüttük

        // Arka plan resmi (eğer varsa)
        ImageView backgroundView = new ImageView(backgroundImage); // Veya farklı bir arka plan resmi
        root.getChildren().add(backgroundView);

        // Butonları içerecek VBox
        VBox buttonBox = new VBox(20); // Butonlar arası boşluk
        buttonBox.setPadding(new Insets(20)); // Kenarlardan boşluk
        buttonBox.setAlignment(Pos.CENTER); // Butonları ortala

        // Başlık
        Label titleLabel = new Label("Lobi Seçenekleri");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 4,0,0,1);");

        // "Lobiye Katıl" butonu
        Button joinLobbyButton = new Button("Lobiye Katıl");
        joinLobbyButton.setPrefWidth(200); // Buton genişliği
        joinLobbyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"); // Yeşil buton
        joinLobbyButton.setOnAction(event -> showAvailableLobbies(primaryStage));

        // "Lobi Oluştur" butonu
        Button createLobbyButton = new Button("Lobi Oluştur");
        createLobbyButton.setPrefWidth(200); // Buton genişliği
        createLobbyButton.setStyle("-fx-background-color: #008CBA; -fx-text-fill: white;"); // Mavi buton
        createLobbyButton.setOnAction(event -> createLobby(primaryStage));

        buttonBox.getChildren().addAll(titleLabel, joinLobbyButton, createLobbyButton);
        root.getChildren().add(buttonBox);

        primaryStage.setScene(scene);
    }


    private void showAvailableLobbies(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20)); // Kenarlardan boşluk ekle
        root.setAlignment(Pos.CENTER); // Elemanları ortala
        Scene scene = new Scene(root, 800, 600); // Pencere boyutunu büyüt

        // Başlık
        Label titleLabel = new Label("Mevcut Lobiler");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#333")); // Koyu gri renk

        // Lobi Listesi (Görünümünü iyileştirme)
        ListView<String> lobbiesListView = new ListView<>();
        lobbiesListView.setPrefHeight(150); // Liste yüksekliği
        lobbiesListView.setStyle("-fx-control-inner-background: #f0f0f0; -fx-font-size: 14px;"); // Açık gri arka plan ve font boyutu

        // Butonlar (Stillerini iyileştirme)
        Button refreshButton = new Button("Yenile");
        refreshButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;"); // Mavi buton

        refreshButton.setOnAction(event -> gameClient.requestAvailableLobbies());

        Button joinButton = new Button("Katıl");
        joinButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;"); // Yeşil buton
        joinButton.setOnAction(event -> {
            lobbyId = lobbiesListView.getSelectionModel().getSelectedItem();
            if (lobbyId != null) {
                joinLobby(lobbyId, primaryStage);
            }
        });

        Button returnButton = new Button("Geri");
        returnButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;"); // Kırmızı buton
        returnButton.setOnAction(event -> showLobbyOptions(primaryStage));

        // Butonları yatay olarak düzenlemek için HBox
        HBox buttonBox = new HBox(10); // Butonlar arası boşluk
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(refreshButton, joinButton, returnButton);

        gameClient.setLobbyUpdateCallback(lobbies -> Platform.runLater(() -> {
            System.out.println("Updating lobby list: " + lobbies); // Debugging line
            lobbiesListView.getItems().setAll(lobbies);
        }));

        root.getChildren().addAll(titleLabel, lobbiesListView, buttonBox); // Düzenleme

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
        System.out.println("Lobby ID: " + lobbyId);
        System.out.println("Created lobby with ID: " + lobbyId);
        showLobbyScreenFromCreate(primaryStage, gameClient);
    }

    private void showLobbyScreenFromCreate(Stage primaryStage, GameClient gameClient) {
        BorderPane root = new BorderPane(); // Esnek düzen için BorderPane
        Scene scene = new Scene(root, 800, 600);



        lobbyPlayersTextArea = new TextArea();
        lobbyPlayersTextArea.setEditable(false);
        lobbyPlayersTextArea.setPrefHeight(180);
        lobbyPlayersTextArea.setStyle("-fx-control-inner-background: #f0f0f0; -fx-font-size: 14px;"); // Açık gri arka plan ve font boyutu

        VBox chatBox = new VBox(5); // Mesaj kutusu ve gönder butonu arası boşluk
        chatBox.setPadding(new Insets(10)); // Kenarlardan boşluk

        chatField = new TextField();
        chatField.setPromptText("Mesajınızı yazın...");
        chatField.setStyle("-fx-background-color: white;");

        Button sendButton = new Button("Gönder");
        sendButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;"); // Mavi buton

        chatBox.getChildren().addAll(chatField, sendButton);


        Button startGameButton = new Button("Oyunu Başlat");
        startGameButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"); // Yeşil buton
        startGameButton.setOnAction(event -> startGame(primaryStage));

        // Elemanları yerleştirme

        root.setCenter(lobbyPlayersTextArea);
        root.setBottom(chatBox);
        root.setRight(startGameButton); // Sağ tarafa yerleştirme
        BorderPane.setMargin(startGameButton, new Insets(0, 10, 10, 0)); // Sağdan ve alttan boşluk

        gameClient.setLobbyCreatedCallback(lobbyId -> Platform.runLater(() -> {
            Label titleLabel = new Label("Lobi: "+ lobbyId);
            titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            titleLabel.setPadding(new Insets(10));
            root.setTop(titleLabel);
        }));


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
        primaryStage.setScene(scene);
    }

    private void showLobbyScreenFromJoin(Stage primaryStage, GameClient gameClient) {
        BorderPane root = new BorderPane(); // Esnek düzen için BorderPane
        Scene scene = new Scene(root, 800, 600);

        Label titleLabel = new Label("Lobi");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setPadding(new Insets(10));

        lobbyPlayersTextArea = new TextArea();
        lobbyPlayersTextArea.setEditable(false);
        lobbyPlayersTextArea.setPrefHeight(180);
        lobbyPlayersTextArea.setStyle("-fx-control-inner-background: #f0f0f0; -fx-font-size: 14px;"); // Açık gri arka plan ve font boyutu

        VBox chatBox = new VBox(5); // Mesaj kutusu ve gönder butonu arası boşluk
        chatBox.setPadding(new Insets(10)); // Kenarlardan boşluk

        chatField = new TextField();
        chatField.setPromptText("Mesajınızı yazın...");
        chatField.setStyle("-fx-background-color: white;");

        Button sendButton = new Button("Gönder");
        sendButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;"); // Mavi buton

        chatBox.getChildren().addAll(chatField, sendButton);



        // Elemanları yerleştirme
        root.setTop(titleLabel);
        root.setCenter(lobbyPlayersTextArea);
        root.setBottom(chatBox);

        gameClient.setLobbyCreatedCallback(lobbyId -> Platform.runLater(() -> {
            root.getChildren().add(new Label("Lobby: " + lobbyId));
        }));


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
                        gameStateType = GameStateType.GameGoingOn;
                        showGameScreen(primaryStage);
                    }
                });
            }
        });

        primaryStage.setScene(scene);
    }

    private void startGame(Stage primaryStage) {
        gameStateType = GameStateType.GameGoingOn;
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



        gameClient.setGameUpdateCallback(gameState -> {
            Platform.runLater(() -> {
                if (gameState.getType().equals(GameStateType.GameGoingOn)) {
                    gameStateType= GameStateType.GameGoingOn;
                    drawGameState(gameState);
                    updatePlayerStats(gameState);
                    // System.out.println("Game is going on: " + gameStateType);

                } else if(gameState.getType().equals(GameStateType.GameOver)){
                    lobbyId= null;
                    gameStateType= GameStateType.GameOver;
                    List<String> gameScores = gameState.getGameScores();
                    gameOverScreen(primaryStage, gameScores);

                }
            });
        });

        scene.setOnKeyPressed(this::handleKeyPressed);
        scene.setOnKeyReleased(this::handleKeyReleased);
        if (gameStateType.equals(GameStateType.GameGoingOn)) {
            startGameLoop();
        } else {
            System.out.println("gameloop stop");
            gameLoop.stop();
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
                //System.out.println("Health: " + ship.getHealth() + ", Score: " + ship.getScore());
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
        shipX = 400; // Varsayılan başlangıç X koordinatı
        shipY = 300; // Varsayılan başlangıç Y koordinatı


        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

               ; // convert nanoseconds to seconds
                lastUpdate = now;

                double deltaX = 0;
                double deltaY = 0;

                if (pressedKeys.contains(KeyCode.UP)) {
                    deltaY -= SHIP_SPEED ;
                }
                if (pressedKeys.contains(KeyCode.DOWN)) {
                    deltaY += SHIP_SPEED ;
                }
                if (pressedKeys.contains(KeyCode.LEFT)) {
                    deltaX -= SHIP_SPEED ;
                }
                if (pressedKeys.contains(KeyCode.RIGHT)) {
                    deltaX += SHIP_SPEED ;
                }

                shipX +=  deltaX;
                shipY +=  deltaY;


                if ((deltaX != 0 || deltaY != 0 ) && gameStateType.equals(GameStateType.GameGoingOn) ) {
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
