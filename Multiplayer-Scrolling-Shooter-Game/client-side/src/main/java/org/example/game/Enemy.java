package org.example.game;

public class Enemy extends GameObject {
    private int health;
    private int speed;

    public Enemy(int id, double x, double y, int health) {
        super(id, x, y, 50, 50); // Örnek olarak genişlik ve yükseklik 50 olarak ayarlandı
        this.health = health;

    }

    public int getHealth() {
        return health;
    }

    public void decreaseHealth() {
        health--;
    }

    public void update() {
        setY(getY() + speed); // Düşmanı aşağı hareket ettir

    }

}
