package org.example.game;

public class Bullet extends GameObject {
    private int speed;

    public Bullet(double x, double y, int speed) {
        super(0, x, y, 20, 30); // Örnek olarak genişlik ve yükseklik 10 olarak ayarlandı
        this.speed = speed;
    }

    public void update() {
       setY(getY() - speed); // Mermiyi yukarı hareket ettir
    }

    // Diğer metotlar...
}
