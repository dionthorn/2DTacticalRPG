package org.dionthorn.javafxshellexample;

public class Light {

    public static int LIGHT_SIZE = 5;
    private int x;
    private int y;
    private int lastMoved = 0;

    public Light(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getLastMoved() {
        return lastMoved;
    }

    public void setLastMoved(int lastMoved) {
        this.lastMoved = lastMoved;
    }

}
