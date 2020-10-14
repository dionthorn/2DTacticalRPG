package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class ItemOnMap extends Item implements Drawable {

    private final int x;
    private final int y;

    public ItemOnMap(String name, int value, double weight, int x, int y, Image sprite) {
        super(name, value, weight, sprite);
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public void draw(GraphicsContext gc) {
        // need to setup methods for relativeX/Y
        gc.drawImage(getSprite(), x * Run.TILE_SIZE, y * Run.TILE_SIZE, Run.TILE_SIZE, Run.TILE_SIZE);
    }

    // Need a toInventory method to turn this on map item into an InInventory item.
    public ItemInInventory toInInventory() {
        // simply strips the x,y attributes as an inventory item doesn't need such information
        return new ItemInInventory(getName(), getValue(), getWeight(), getSprite());
    }

}