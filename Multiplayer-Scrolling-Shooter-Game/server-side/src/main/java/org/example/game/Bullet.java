package org.example.game;

import org.example.server.ClientHandler;

public class Bullet extends GameObject {
    private ClientHandler owner;
    private int speed;

    public Bullet(double x, double y, int speed, ClientHandler owner) {
        super(0, x, y, 10, 10); // Örnek olarak genişlik ve yükseklik 10 olarak ayarlandı
        this.speed = speed;
        this.owner = owner;
    }

    public void update() {
        setY(getY() - speed); // Mermiyi yukarı hareket ettir
    }

    public ClientHandler getOwner() {
        return owner;
    }

    // Diğer metotlar...
}
