package org.dionthorn;

public class MapTile {

    public enum TileType { DEFAULT, IMPASSABLE, FIRE }

    private int tileSet, tileID;
    private TileType type;

    public MapTile(int set, int id) {
        tileSet = set;
        tileID = id;
        type = TileType.DEFAULT;
    }

    public void tagFire() { type = TileType.FIRE;}

    public void tagImpassable() { type = TileType.IMPASSABLE;}

    public int getTileSet() { return tileSet; }

    public TileType getType() { return type;}

    public int getTileID() { return tileID; }

    public void setTileID(int id) { tileID = id; }

    public void setTileSet(int set) { tileSet = set; }


}

