package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class ItemOnMap extends Item implements Drawable {

    private int x;
    private int y;
    private Image sprite;

    public ItemOnMap(int value, double weight, int x, int y, Image sprite) {
        super(value, weight);
    }

    @Override
    public void draw(GraphicsContext gc) {
        // need to setup methods for relativeX/Y
        gc.drawImage(sprite, x * Run.TILE_SIZE, y * Run.TILE_SIZE, Run.TILE_SIZE, Run.TILE_SIZE);
    }

}
