package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.net.URI;

public class ItemOnMap extends Item implements Drawable {

    private final int x;
    private final int y;

    public ItemOnMap(String name, int value, double weight, int x, int y, Image sprite) {
        super(name, value, weight, sprite);
        this.x = x;
        this.y = y;
    }

    public static ItemOnMap makeItemOnMap(String path, String name, int x, int y) {
        // path is the name of the .dat file for item definitions. 0_baseItems.dat should be default
        int value = 0;
        double weight = 0;
        String spritePath = "";
        int tileId = 0;
        // uses just the path to the item set and the unique item name within the set, then loads off memory
        String[] itemsDefs = FileOpUtils.getFileLines(URI.create(Run.GAME_ITEM_PATH + path + ".dat"));
        for(String itemDef: itemsDefs) {
            String[] info = itemDef.split(",");
            if(info[0].equals(name)) {
                System.out.println("FOUND TARGET ITEM: " + name);
                value = Integer.parseInt(info[1]);
                weight = Double.parseDouble(info[2]);
                spritePath = info[3].split(":")[0];
                tileId = Integer.parseInt(info[3].split(":")[1]);
                break;
            }
        }
        System.out.println("Item/" + spritePath);
        Image sprite = new TileSet("Item/" + spritePath, Run.TILE_SIZE).getTile(tileId);
        return new ItemOnMap(name, value, weight, x, y, sprite);
    }

    public int getRelativeX() { return x - RenderUtil.anchorUL[0]; }

    public int getRelativeY() { return y - RenderUtil.anchorUL[1]; }

    @Override
    public void draw(GraphicsContext gc) {
        // need to setup methods for relativeX/Y
        gc.drawImage(getSprite(), getRelativeX() * Run.TILE_SIZE, getRelativeY() * Run.TILE_SIZE, Run.TILE_SIZE, Run.TILE_SIZE);
    }

    // Need a toInventory method to turn this on map item into an InInventory item.
    public ItemInInventory toInInventory() {
        // simply strips the x,y attributes as an inventory item doesn't need such information
        return new ItemInInventory(getName(), getValue(), getWeight(), getSprite());
    }

}