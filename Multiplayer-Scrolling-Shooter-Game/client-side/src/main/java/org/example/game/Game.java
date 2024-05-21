package org.example.game;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class Game {
    private List<Ship> ships;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private Canvas canvas;

    public Game(Canvas canvas) {
        ships = new ArrayList<>();
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        this.canvas = canvas;
    }

    public void updateFromServer(String message) {
        Gson gson = new Gson();
        GameState gameState = gson.fromJson(message, GameState.class);

        ships = gameState.getShips();
        enemies = gameState.getEnemies();
        bullets = gameState.getBullets();

        Platform.runLater(this::renderGame);
    }

    private void renderGame() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Gemileri çiz
        for (Ship ship : ships) {
            gc.setFill(Color.BLUE);
            gc.fillRect(ship.getX(), ship.getY(), ship.getWidth(), ship.getHeight());
        }

        // Düşmanları çiz
        for (Enemy enemy : enemies) {
            gc.setFill(Color.RED);
            gc.fillRect(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());
        }

        // Mermileri çiz
        for (Bullet bullet : bullets) {
            gc.setFill(Color.BLACK);
            gc.fillRect(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight());
        }
    }

    public List<Ship> getShips() {
        return ships;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }
}