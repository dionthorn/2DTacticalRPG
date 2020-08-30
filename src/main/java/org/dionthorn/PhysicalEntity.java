package org.dionthorn;

/**
 * The PhysicalEntity abstract class gives an Entity its Map and X,Y attributes
 */
public abstract class PhysicalEntity extends Entity {

    protected int x, y;
    protected Map currentMap;

    /**
     * The Default Constructor for the Physical Entity will assign Map as well as x,y
     * @param map sets the entities associated map
     * @param x sets the default x location
     * @param y sets the default y location
     */
    protected PhysicalEntity(Map map, int x, int y) {
        currentMap = map;
        this.x = x;
        this.y = y;
    }

    /**
     * Will Move this entity in the provided x and y directions
     * These can be positive 1, 0, or -1. Ex to move left once: move(-1,0)
     * Collision Detection should be performed before calling move()
     * @param xDir the x-wards direction 1, 0, or -1
     * @param yDir the y-wards direction 1, 0, or -1
     */
    protected void move(int xDir, int yDir) {
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
    /**
     * Returns this entities x location.
     * @return returns this entities x location
     */
    protected int getX() { return x; }

    /**
     * Returns this entities y location.
     * @return returns this entities y location
     */
    protected int getY() { return y; }

    protected int getRealtiveX() {
        return x - Render.anchorUL[0];
    }

    protected int getRealtiveY() {
        return y - Render.anchorUL[1];
    }

    /**
     * Assigns this entity a new x location.
     * @param newX Assigns this entity a new x location
     */
    protected void setX(int newX) { x = newX; }

    /**
     * Assigns this entity a new y location.
     * @param newY Assigns this entity a new y location
     */
    protected void setY(int newY) { y = newY; }

    /**
     * Returns the Map associated with this entity.
     * @return the map associated with this entity
     */
    protected Map getCurrentMap() { return currentMap; }

    /**
     * Changes the associated Map of this entity.
     * @param newMap the Map the entity will move to
     * @param x the x location the entity should be on for the new map
     * @param y the y location the entity should be on for the new map
     */
    protected void setCurrentMap(Map newMap, int x, int y) {
        currentMap = newMap;
        this.x = x;
        this.y = y;
    }

}

