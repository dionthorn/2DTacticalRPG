package org.dionthorn;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;

/**
 * A TileSet is a .png file saved on Disk that can be split into 32*32 pixel tiles.
 * A TileID is the occurrence of the tile in the sheet going from left to right, from the top to bottom:
 * [ 0, 1, 2,
 *   3, 4, 5,
 *   6, 7, 8 ]
 * A TileSet can also be a sprite sheet of 32*32 pixel character images.
 * The LAST image of a tileSet must be a 'blank' tile.
 * If the entire tileset isn't composed of unique images,
 * all other unused space must be the same image as the 'blank' tile.
 * All tiles matching the 'blank' tile will be automatically trimmed to save space in the Image[].
 * All tileSets should be found in GameData/Art.
 */
public class TileSet {

    private static final ArrayList<TileSet> tileSetCache = new ArrayList<>();
    private final String tileSetPath;
    private final Image tileSetSrc;
    private Image[] tiles;
    private Image blank;
    private int totalTiles;
    private final ArrayList<Integer> removedTileIDList = new ArrayList<>();
    private final ArrayList<Integer> metaFire = new ArrayList<>();
    private final ArrayList<Integer> metaImpassable = new ArrayList<>();

    /**
     * Constructor creates the TileSet object from a path to a .png file and the size of the squares to cut.
     * @param path the relative String that points to the desired .png file on disk
     * @param TILE_SIZE the size of the squares to cut the image into default 32
     */
    public TileSet(String path, int TILE_SIZE) {
        boolean sameFound = false;
        int sameIndex = 0;
        for(int i=0; i<tileSetCache.size(); i++) {
            if(tileSetCache.get(i).getTileSetPath().equals(path)) {
                sameFound = true;
                sameIndex = i;
                break;
            }
        }
        boolean useMOD = false;
        if(path.contains("MOD.")) {
            System.out.println("USE MOD");
            useMOD = true;
            System.out.println(path);
        }
        tileSetPath = path;
        System.out.println(tileSetPath);
        if(sameFound) {
            tileSetSrc = tileSetCache.get(sameIndex).getTileSetSrc();
            tiles = tileSetCache.get(sameIndex).getTiles();
            blank = tileSetCache.get(sameIndex).getBlank();
            Integer[] boxedArray = Arrays.stream(
                    tileSetCache.get(sameIndex).getRemovedTileID()
            ).boxed().toArray(Integer[]::new);
            Collections.addAll(removedTileIDList, boxedArray);
            totalTiles = tileSetCache.get(sameIndex).getTotalTiles();
        } else {
            // (useJRT ? Run.MOD_ART_PATH + tileSetPath : Run.GAME_ART_PATH + tileSetPath
            System.out.println("USING: " + (useMOD ? Run.MOD_ART_PATH : Run.GAME_ART_PATH) + "/" + tileSetPath);
            tileSetSrc = new Image((useMOD ? Run.MOD_ART_PATH : Run.GAME_ART_PATH) + "/" + tileSetPath);
            tiles = makeTiles(tileSetSrc, TILE_SIZE);
            if (tiles.length == 0) {
                Run.programLogger.log(Level.SEVERE, "NO TILES DETECTED");
            } else {
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
                Run.programLogger.log(Level.INFO, String.format("Tiles Removed From %s: %d", path, removedTilesCount));
            }
            TileSet.tileSetCache.add(this);
            tileSetCache.trimToSize();
        }
    }
    // Getters and Setters
    /**
     * Returns an integer array representing the id of where 'blank' duplicate tiles would be on the full image.
     * @return the integer array of tileIDs of 'blank' duplicates.
     */
    public int[] getRemovedTileID() {
        int[] toReturn = new int[removedTileIDList.size()];
        int step = 0;
        for(Integer i: removedTileIDList) {
            int ID = i;
            toReturn[step] = ID;
            step++;
        }
        return toReturn;
    }

    /**
     * Returns the TileSet objects relative path as a String object.
     * @return the tileset path as a string.
     */

    public String getTileSetPath() { return tileSetPath; }
    /**
     * Returns the full source image stored for the TileSet.
     * @return the image of the original full tileset
     */
    public Image getTileSetSrc() { return tileSetSrc; }

    /**
     * Returns an image array of all the tiles associated with the TileSet.
     * @return the image array of all the tileset tiles
     */
    public Image[] getTiles() { return tiles; }

    /**
     * Returns the Image object of a tile specified by its tileID.
     * @param tileID the index of the tile
     * @return the image of the tile specified by tileID
     */
    public Image getTile(int tileID) { return tiles[tileID]; }

    /**
     * Returns the count of the usable Tiles in the TileSet.
     * @return the integer reference to the total usable tiles in the set
     */
    public int getTotalTiles() { return totalTiles; }

    /**
     * Returns the Image object in the TileSet that represents the 'blank' Tile.
     * The 'blank' tile will be whatever the last square in the image is, it is used to remove duplicate 'blank' tiles.
     * @return the image of the 'blank' tile in the tileset
     */
    public Image getBlank() { return blank; }

    public ArrayList<Integer> getMetaFire() { return metaFire; }

    public ArrayList<Integer> getMetaImpassable() { return metaImpassable; }

    public void setMetaFireID(int tileID, boolean remove) {
        metaFire.trimToSize();
        boolean found = false;
        if(!remove) {
            for(int i: metaFire) {
                if(i == tileID) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                metaFire.add(tileID);
            }
        } else {
            int target = 0;
            for(int index=0; index<metaFire.size(); index++) {
                if(metaFire.get(index) == tileID) {
                    found = true;
                    target = index;
                    break;
                }
            }
            if(found) {
                metaFire.remove(target);
            }
        }
    }

    public void setMetaImpassableID(int tileID, boolean remove) {
        boolean found = false;
        if(!remove) {
            for(int i: metaImpassable) {
                if(i == tileID) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                metaImpassable.add(tileID);
            }
        } else {
            int target = 0;
            for(int index=0; index<metaImpassable.size(); index++) {
                if(metaImpassable.get(index) == tileID) {
                    found = true;
                    target = index;
                    break;
                }
            }
            if(found) {
                metaImpassable.remove(target);
            }
        }
    }

    // Static Methods
    /**
     * Processes a full Image tileset into an Image[] where each Image in the array is a square where all sides are
     * of length TILE_SIZE.
     * @param tileSet the full size image of the tileset
     * @param TILE_SIZE the size of the squares that the image will be cut into
     * @return an image array of all the tiles in the provided tileset where each tile is of size tile_size squared
     */
    private static Image[] makeTiles(Image tileSet, int TILE_SIZE) {
        int srcW = (int) tileSet.getWidth();
        int srcH = (int) tileSet.getHeight();
        int maxTiles = ((srcW / TILE_SIZE) * (srcH/ TILE_SIZE));
        int maxTilesWidth = (srcW / TILE_SIZE);
        Image[] toReturn = new Image[maxTiles];
        PixelReader pr = tileSet.getPixelReader();
        PixelWriter pw;
        int wCount = 0;
        int hCount = 0;
        int xOffset;
        int yOffset;
        for(int i=0; i<maxTiles; i++) {
            WritableImage wImg = new WritableImage(TILE_SIZE, TILE_SIZE);
            pw = wImg.getPixelWriter();
            if(wCount >= maxTilesWidth) {
                wCount = 0;
                hCount++;
            }
            xOffset = wCount * TILE_SIZE;
            yOffset = hCount * TILE_SIZE;
            for(int readY = 0; readY < TILE_SIZE; readY++) {
                for(int readX = 0; readX < TILE_SIZE; readX++) {
                    Color color = pr.getColor(readX + xOffset, readY + yOffset);
                    pw.setColor(readX, readY, color);
                }
            }
            toReturn[i] = wImg;
            wCount++;
        }
        return toReturn;
    }

    /**
     * Compares two Images pixel by pixel for color, if every pixel is the same it returns true, otherwise false.
     * @param a first image to compare
     * @param b second image to compare
     * @return true if the images are per pixel matches
     */
    public static boolean areImagesSame(Image a, Image b) {
        if(a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight()) {
            for(int x=0; x<(int) a.getWidth(); x++) {
                for(int y=0; y<(int) a.getHeight(); y++) {
                    if(!a.getPixelReader().getColor(x, y).equals(b.getPixelReader().getColor(x, y))) return false;
                }
            }
        }
        return true;
    }

    /**
     * Removes all Images in the original Image[] that match the Image provided in toRemove.
     * toRemove is preserved at the end of the array, this is intended to be the 'blank' tile in the set.
     * @param original the image array to search through
     * @param toRemove the image to remove from the array
     * @return returns a new image array where all images matching toRemove inside the original array are removed,
     *         toRemove is appended
     */
    private static Image[] removeSameElements(Image[] original, Image toRemove) {
        ArrayList<Image> toReturn = new ArrayList<>();
        for(Image i: original) {
            if (!areImagesSame(i, toRemove)) {
                toReturn.add(i);
            }
        }
        toReturn.add(toRemove);
        toReturn.trimToSize();
        Image[] newArray = new Image[toReturn.size()];
        return toReturn.toArray(newArray);
    }

}

