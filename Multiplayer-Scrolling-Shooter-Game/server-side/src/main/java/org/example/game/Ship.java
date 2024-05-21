package org.example.game;

public class Ship extends GameObject {
    private int health;
    private int score;

    public Ship(int id, double x, double y, int health) {
        super(id, x, y, 50, 50); // Örnek olarak genişlik ve yükseklik 50 olarak ayarlandı
        this.health = health;
        this.score = 0;
    }

    public int getHealth() {
        return health;
    }

    public void decreaseHealth() {
        health--;
    }

    public int getScore() {
        return score;
    }

    public void increaseScore() {
        score++;
    }

    // Diğer metotlar...
}
