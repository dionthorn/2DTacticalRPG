package org.dionthorn;

public abstract class PhysicalEntity extends Entity {

    protected int x, y;       // Entities location in 2D space in Tile Units
    protected Map currentMap; // The map that the Entity is connected to

    protected PhysicalEntity(Map map, int x, int y) {
        currentMap = map;
        this.x = x;
        this.y = y;
    }

    protected void move(int xDir, int yDir) {
        // Will move the entity to the (x,y) location, Collision detection should be done before using .move()
        if(this.x + xDir >= currentMap.getMapWidth()) {
            xDir = 0;
        }
        if(this.x + xDir < 0) {
            xDir = 0;
        }
        if(this.y + yDir >= currentMap.getMapHeight()) {
            yDir = 0;
        }
        if(this.y + yDir < 0) {
            yDir = 0;
        }
        setX(this.x + xDir);
        setY(this.y + yDir);
    }
    // Getters and Setters
    protected int getX() {
        return x;
    }

    protected int getY() { return y; }

    protected void setX(int newX) {
        x = newX;
    }

    protected void setY(int newY) {
        y = newY;
    }

    protected Map getCurrentMap() {
        return currentMap;
    }

    protected void setCurrentMap(Map newMap, int x, int y) {
        // Moves the entity to a new map
        currentMap = newMap;
        this.x = x;
        this.y = y;
    }

}

