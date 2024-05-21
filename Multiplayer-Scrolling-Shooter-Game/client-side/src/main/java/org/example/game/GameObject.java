package org.example.game;

public abstract class GameObject {
    private int id;
    private double x;
    private double y;
    private int width;
    private int height;

    public GameObject(int id, double x, double y, int width, int height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void update() {
        // Nesne güncelleme mantığı
    }
}
