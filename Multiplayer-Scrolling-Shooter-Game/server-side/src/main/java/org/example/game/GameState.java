package org.example.game;

import java.util.List;

public class GameState {
    private List<Ship> ships;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private List<String> gameScores;
    private GameStateType type;

    public GameStateType getType() {
        return type;
    }

    public void setType(GameStateType type) {
        this.type = type;
    }

    public List<String> getGameScores() {
        return gameScores;
    }

    public void setGameScores(List<String> gameScores) {
        this.gameScores = gameScores;
    }

    public List<Ship> getShips() {
        return ships;
    }

    public void setShips(List<Ship> ships) {
        this.ships = ships;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public void setEnemies(List<Enemy> enemies) {
        this.enemies = enemies;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public void setBullets(List<Bullet> bullets) {
        this.bullets = bullets;
    }
}
