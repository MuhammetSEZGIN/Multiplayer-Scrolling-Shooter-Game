package org.example.game;

public class Enemy extends GameObject {
    private int health;
    private int shootCooldown;

    public Enemy(int id, double x, double y, int health) {
        super(id, x, y, 50, 50); // Örnek olarak genişlik ve yükseklik 50 olarak ayarlandı
        this.health = health;
        this.shootCooldown = 0;
    }

    public int getHealth() {
        return health;
    }

    public void decreaseHealth() {
        health--;
    }

    public void update() {
        // Düşman hareketi mantığı
        if (shootCooldown > 0) {
            shootCooldown--;
        } else {
            shoot();
            shootCooldown = 100; // Örnek olarak mermi atma aralığı
        }
    }

    private void shoot() {
        // Düşmandan mermi oluştur ve oyuna ekle
        Bullet bullet = new Bullet(getX(), getY(), -10); // Düşman mermisi yukarı hareket eder
        // Oyuna mermi ekleme mantığı burada olmalı
    }
}
