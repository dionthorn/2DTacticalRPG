package org.dionthorn;

/**
 * The MapTile class will be used to track individual tile attributes
 */
public class MapTile {

    /**
     * Enumerator to track TileType
     * Fire will cause entities to take damage
     * Impassable prevents entities from occupying the location
     */
    public enum TileType { DEFAULT, IMPASSABLE, FIRE }

    private int tileSet, tileID;
    private TileType type;

    /**
     * Default Constructor for a MapTile assigns its tileSet id and TileID within the set.
     * type is set to DEFAULT but can be tagged post initialization.
     * @param set the integer index of the associated tileSet
     * @param id the integer index of the associated tileID within the tileSet
     */
    public MapTile(int set, int id) {
        tileSet = set;
        tileID = id;
        type = TileType.DEFAULT;
    }

    /**
     * Tags this MapTile as being of the type Fire
     */
    public void tagFire() { type = TileType.FIRE;}

    /**
     * Tags this MapTile as being of the type Impassable
     */
    public void tagImpassable() { type = TileType.IMPASSABLE;}

    /**
     * Returns the integer index of this MapTiles associated tileSet
     * @return the integer index of this MapTiles associated tileSet
     */
    public int getTileSet() { return tileSet; }

    /**
     * Returns this MapTiles associated TileType
     * @return the TileType of this MapTile
     */
    public TileType getType() { return type;}

    /**
     * Returns this MapTiles associated tileID index as an integer
     * @return an integer representing this MapTiles tileID of its associated tileSet
     */
    public int getTileID() { return tileID; }

    /**
     * sets tileID of this MapTile
     * @param id an integer for the index of the tileID associated to this MapTile
     */
    public void setTileID(int id) { tileID = id; }

    /**
     * Sets the associated tileSet of this MapTile
     * @param set an integer for the index of the tileSet associated to this MapTile
     */
    public void setTileSet(int set) { tileSet = set; }

}

