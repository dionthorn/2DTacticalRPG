package org.dionthorn;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
/*
    A TileSet is a .png file saved on Disk that can be split into 32*32 pixel tiles
    A TileID is the occurrence of the tile in the sheet going from left to right, from the top to bottom
    [ 0, 1, 2,
      3, 4, 5,
      6, 7, 8 ]
    A TileSet can also be a sprite sheet of 32*32 pixel character images
    The LAST image of a tileSet must be a 'blank' tile
    If the entire tileset isn't composed of unique images all other unused space must be the same image as the 'blank' tile
    All tiles matching the 'blank' tile will be automatically trimmed to save space in the Image[]
    All tileSets should be found in GameData/Art
 */
public class TileSet {
    // tileSetCache is a static list of all created tileSets
    private static ArrayList<TileSet> tileSetCache = new ArrayList<>();
    private String tileSetPath; // The relative file path of the tileSet should be in /Art
    private Image tileSetSrc;   // A copy of the full tileSet
    private Image[] tiles;      // Image[] of all the separated tiles in the tileSet
    private Image blank;        // A copy of the 'blank' tile
    private int totalTiles;     // Number of tiles in set after removing excess 'blanks'
    private ArrayList<Integer> removedTileIDList = new ArrayList<>(); // Used to track which tileIDs are same as blank

    public TileSet(String path, int TILE_SIZE) {
        // A tile set requires only a relative path and the tileSize (32 fixed for now)
        // See if tileSet already exists
        boolean sameFound = false;
        int sameIndex = 0;
        for(int i=0; i<tileSetCache.size(); i++) {
            if(tileSetCache.get(i).getTileSetPath().equals(path)) {
                sameFound = true;
                sameIndex = i;
                break;
            }
        }
        if(sameFound) {
            // If it exists we will copy already existing info over.
            tileSetPath = path;
            tileSetSrc = tileSetCache.get(sameIndex).getTileSetSrc();
            tiles = tileSetCache.get(sameIndex).getTiles();
            blank = tileSetCache.get(sameIndex).getBlank();
            Integer[] boxedArray = Arrays.stream(tileSetCache.get(sameIndex).getRemovedTileID()).boxed().toArray(Integer[]::new);
            Collections.addAll(removedTileIDList, boxedArray);
            totalTiles = tileSetCache.get(sameIndex).getTotalTiles();
        } else {
            // If it doesn't already exist in cache then attempt to load from disk
            tileSetPath = path;
            tileSetSrc = new Image("file:"+ Run.GAME_DATA_PATH + "/Art/" + path);
            tiles = makeTiles(tileSetSrc, TILE_SIZE);
            // The last tile of a tileset should be the 'blank' tile for the set
            // To compare other tiles with for reducing Image[] size
            if (tiles.length == 0) {
                // Something has gone horribly wrong.
                System.err.println("NO TILES DETECTED");
            } else {
                // If nothing bad happened from makeTiles() then we should be good
                blank = tiles[tiles.length - 1];
                int sizeOriginal = tiles.length;
                for (int step = 0; step < tiles.length; step++) {
                    if (areImagesSame(tiles[step], blank)) {
                        removedTileIDList.add(step);
                    }
                }
                tiles = removeSameElements(tiles, blank);
                int removedTilesCount = sizeOriginal - tiles.length;
                totalTiles = tiles.length;
                if(Run.DEBUG_OUTPUT) {
                    System.out.println(String.format("Tiles Removed: %d", removedTilesCount));
                }
            }
            TileSet.tileSetCache.add(this); // After finishing the tileSet we add to master cache
            tileSetCache.trimToSize();
        }
    }
    // Getters and Setters
    public int[] getRemovedTileID() {
        // returns an int[] of the 'TileID' of the removed tiles off the sheet
        int[] toReturn = new int[removedTileIDList.size()];
        int step = 0;
        for(Integer i: removedTileIDList) {
            int ID = i;
            toReturn[step] = ID;
            step++;
        }
        return toReturn;
    }

    public String getTileSetPath() { return tileSetPath; }

    public Image getTileSetSrc() { return tileSetSrc; }

    public Image[] getTiles() { return tiles; }

    public Image getTile(int tileID) { return tiles[tileID]; }

    public int getTotalTiles() { return totalTiles; }

    public Image getBlank() { return blank; }

    // Static Methods
    private static Image[] makeTiles(Image tileSet, int TILE_SIZE) {
        // This is the most complicated part of the program probably.
        int srcW = (int) tileSet.getWidth(); // Get the source Images width/height
        int srcH = (int) tileSet.getHeight();
        int maxTiles = ((srcW / TILE_SIZE) * (srcH/ TILE_SIZE)); // Calculate the maximum amount of tiles
        int maxTilesWidth = (srcW / TILE_SIZE); // Calc max tiles width
        Image[] toReturn = new Image[maxTiles]; // Prep an Image[] which will be the final returned array
        PixelReader pr = tileSet.getPixelReader(); // Need a pixel reader and writer to read and create Images
        PixelWriter pw;
        int wCount = 0; // Tracking variables
        int hCount = 0;
        int xOffset;
        int yOffset;
        for(int i=0; i<maxTiles; i++) {
            WritableImage wImg = new WritableImage(TILE_SIZE, TILE_SIZE); // Need a Writable Image of 32*32 pixels
            pw = wImg.getPixelWriter();
            if(wCount >= maxTilesWidth) {
                wCount = 0;
                hCount++;
            }
            xOffset = wCount * TILE_SIZE;
            yOffset = hCount * TILE_SIZE;
            for(int readY = 0; readY < TILE_SIZE; readY++) {
                for(int readX = 0; readX < TILE_SIZE; readX++) {
                    // Grab the source tile's color for each individual pixel then apply to the writableImage
                    Color color = pr.getColor(readX + xOffset, readY + yOffset);
                    pw.setColor(readX, readY, color);
                }
            }
            toReturn[i] = wImg;
            wCount++;
        }
        return toReturn; // Finally return the resulting Image[]
    }

    public static boolean areImagesSame(Image a, Image b) {
        // Compares two JavaFX Images per pixel with each other by Color
        if(a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight()) {
            for(int x=0; x<(int) a.getWidth(); x++) {
                for(int y=0; y<(int) a.getHeight(); y++) {
                    // If even a single pixel doesn't match color then it will return false
                    if(!a.getPixelReader().getColor(x, y).equals(b.getPixelReader().getColor(x, y))) return false;
                }
            }
        }
        return true;
    }

    private static Image[] removeSameElements(Image[] original, Image toRemove) {
        // Removes on a per pixel & color sameness basis every Image in an Image[] that is the same as toRemove
        // Keeps toRemove in the return array at the last index
        ArrayList<Image> toReturn = new ArrayList<>();
        for(Image i: original) {
            if (!areImagesSame(i, toRemove)) {
                toReturn.add(i);
            }
        }
        toReturn.add(toRemove); // ensure the last Image in the Image[] is the original toRemove
        toReturn.trimToSize(); // ensure size is reduced all the way
        Image[] newArray = new Image[toReturn.size()];
        return toReturn.toArray(newArray);
    }

}

