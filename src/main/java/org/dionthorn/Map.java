package org.dionthorn;

import javafx.scene.image.Image;
import java.util.ArrayList;
import java.util.Random;

/**
 * The Map Class will manage tilesets and tile meta information
 */
public class Map {

    private static int GEN_COUNT = 0;
    private final String PATH;
    private final String metaPATH;
    private String metaEnemies;
    private String metaAllies;
    private String metaStartLoc;
    private int TILE_SIZE = 32;
    private MapTile[][] mapTiles;
    private int mapWidth;
    private int mapHeight;
    private ArrayList<ArrayList<Integer>> mapDataTileMetaIDs = new ArrayList<>();
    private ArrayList<TileSet> tileSets = new ArrayList<>();
    private Image icon;

    /**
     * Default Map Constructor will take a .dat file path and generate based on that,
     * the .meta file is assumed to have the same name as the .dat file.
     * @param datPath the associated .dat file path to generate the map with.
     */
    public Map(String datPath) {
        GEN_COUNT++;
        PATH = datPath;
        String[] tempStr = datPath.split("\\.");
        metaPATH = tempStr[0] + ".meta";
        loadMapData(PATH, metaPATH);
    }

    /**
     * Random Map Constructor, will generate a new map based on the provided tileset paths
     * @param tilePaths the target paths to use for the new random map generation
     */
    public Map(String[] tilePaths) {
        GEN_COUNT++;
        Random rand = new Random();
        PATH = String.format(Run.GAME_DATA_PATH + "/Maps/RANDOM%d.dat", GEN_COUNT);
        int[] mapArea = Run.getMapDimensions();
        mapWidth = mapArea[0];
        mapHeight = mapArea[1];
        for(String path: tilePaths) {
            tileSets.add(new TileSet(path, TILE_SIZE));
        }
        mapTiles = new MapTile[mapHeight][mapWidth];
        for(int y=0; y<mapHeight; y++) {
            for(int x=0; x<mapWidth; x++) {
                int setID = rand.nextInt(tilePaths.length);
                mapTiles[y][x] =  new MapTile(setID, rand.nextInt(tileSets.get(setID).getTiles().length - 1));
            }
        }
        for(int i=0; i< MapTile.TileType.values().length; i++) {
            mapDataTileMetaIDs.add(new ArrayList<>());
        }
        mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).add(0);
        mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).add(0);
        metaAllies = "AO1,5,18,martial/:ALLIES";
        metaEnemies = "MO2,20,19,magic/:ENEMIES";
        metaStartLoc = "5,17,:STARTLOC";
        metaPATH = PATH.split("\\.")[0] + ".meta";
    }

    public void loadMapData(String datPath, String metaPath) {
        String[] tileMetaData = FileOps.getFileLines(metaPATH);
        for(int i=0; i< MapTile.TileType.values().length; i++) {
            mapDataTileMetaIDs.add(new ArrayList<>());
        }
        for(String line: tileMetaData) {
            System.out.println(line);
            if(line.contains("FIRE")) {
                String[] tileIdsFire = line.split(":")[0].split(",");
                for (String s : tileIdsFire) {
                    if (!s.equals("")) {
                        int tempInt = Integer.parseInt(s);
                        mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).add(tempInt);
                    }
                }
            } else if(line.contains("IMPASSABLE")) {
                String[] tileIdsImpassable = line.split(":")[0].split(",");
                for (String s : tileIdsImpassable) {
                    if (!s.equals("")) {
                        int tempInt = Integer.parseInt(s);
                        mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).add(tempInt);
                    }
                }
            } else if(line.contains("ENEMIES")) {
                metaEnemies = line;
            } else if(line.contains("ALLIES")) {
                metaAllies = line;
            } else if(line.contains("STARTLOC")) {
                metaStartLoc = line;
            }
        }
        boolean first = true;
        String[] data = FileOps.getFileLines(PATH);
        int xCount = 0;
        int yCount = 0;
        for(String line: data) {
            String[] splitLine = line.replaceAll(" ", "").split(",");
            if(first) {
                int WIDTH_DATA = 0;
                int HEIGHT_DATA = 1;
                int TILE_DATA = 2;
                int IMG_SRC_DATA = 3;
                mapWidth = Integer.parseInt(splitLine[WIDTH_DATA]);
                mapHeight = Integer.parseInt(splitLine[HEIGHT_DATA]);
                mapTiles = new MapTile[mapHeight][mapWidth];
                TILE_SIZE = Integer.parseInt(splitLine[TILE_DATA]);
                for(int step = IMG_SRC_DATA; step<splitLine.length; step++) {
                    tileSets.add(new TileSet(splitLine[step], TILE_SIZE));
                }
                first = false;
            } else {
                for(String toProcess: splitLine) {
                    String[] finalSplit = toProcess.split(":");
                    mapTiles[yCount][xCount] = new MapTile(Integer.parseInt(finalSplit[0]),
                            Integer.parseInt(finalSplit[1])
                    );
                    boolean cont = true;
                    for(int x = 0; x <mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).size(); x++) {
                        if(mapTiles[yCount][xCount].getTileID() == mapDataTileMetaIDs.get(
                                MapTile.TileType.FIRE.ordinal()).get(x)
                        ) {
                            mapTiles[yCount][xCount].tagFire();
                            cont = false;
                        }
                    }
                    if(cont) {
                        for(int x = 0; x <mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).size(); x++) {
                            if(mapTiles[yCount][xCount].getTileID()==mapDataTileMetaIDs.get(
                                    MapTile.TileType.IMPASSABLE.ordinal()).get(x)
                            ) {
                                mapTiles[yCount][xCount].tagImpassable();
                            }
                        }
                    }
                    xCount++;
                }
                xCount = 0;
                yCount++;
            }
        }
    }

    /**
     * Will generate both a name.meta + name.dat file inside /GameData/Maps
     * this will overwrite any files of the same name.
     * This should be the inverse of the deserial process used by the Map(.dat) constructor.
     * the .dat file has one line of meta data of the form: width,height,tilesize,tilesetpaths
     * such as: 32, 24, 32, 0_mapOne.png, 1_mapTwo.png,
     * where tilesetpaths can be arbitrarily long.
     * The rest of the .dat files will be of the form 1:0, where , is the delimiter
     * of a tile and 1:0 is the tilesetID:tileID.
     * The .meta file will be several lines of meta data where associated data is tagged at the end with a :{type}
     * where type is the meta tag such as 5,7,:STARTLOC where :STARTLOC is the meta tag.
     */
    public void saveData() {
        // Write .dat File
        String[] dataAsString = new String[mapHeight + 1];
        StringBuilder formattedPaths = new StringBuilder();
        for(String path: getTileSetPaths()) {
            formattedPaths.append(path).append(", ");
        }
        dataAsString[0] = String.format("%d, %d, %d, %s", mapWidth, mapHeight, TILE_SIZE, formattedPaths.toString());
        int yCount = 0;
        int xCount = 0;
        int lineCount = 1;
        StringBuilder temp = new StringBuilder();
        for(int i=0; i<mapWidth*mapHeight; i++) {
            int tileID = mapTiles[yCount][xCount].getTileID();
            int tileSetID = mapTiles[yCount][xCount].getTileSet();
            temp.append(String.format("%d:%d, ", tileSetID, tileID));
            xCount++;
            if(xCount == mapWidth) {
                dataAsString[lineCount] = temp.toString();
                temp = new StringBuilder();
                lineCount++;
                xCount = 0;
                yCount++;
            }
        }
        FileOps.writeFileLines(PATH, dataAsString);
        // Write .meta File
        dataAsString = new String[5];
        dataAsString[0] = "";
        for(int i=0; i<mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).size(); i++) {
            dataAsString[0] += mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).get(i) + ",";
        }
        dataAsString[0] += ":FIRE";
        dataAsString[1] = "";
        for(int i=0; i<mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).size(); i++) {
            dataAsString[1] += mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).get(i) + ",";
        }
        dataAsString[1] += ":IMPASSABLE";
        dataAsString[2] = metaEnemies;
        dataAsString[3] = metaAllies;
        dataAsString[4] = metaStartLoc;
        FileOps.writeFileLines(metaPATH, dataAsString);
    }
    // Getters and Setters
    /**
     * Returns an Image of the tile provided by its TileSet index and TileID associated with this Map.
     * @param setIndex the tileset index associated to this map
     * @param tileID the tileid index associated to this map
     * @return the image of the tile at the provided setindex and tileid
     */
    public Image getTile(int setIndex, int tileID) {
        if(setIndex > tileSets.size() - 1) {
            return tileSets.get(0).getBlank();
        } else if(tileID > tileSets.get(setIndex).getTiles().length - 1) {
            return tileSets.get(setIndex).getBlank();
        } else {
            return tileSets.get(setIndex).getTile(tileID);
        }
    }

    /**
     * Returns a int of the Maps tile size in pixels
     * @return the int of the maps tile size in pixels
     */
    public int getTileSize() { return TILE_SIZE; }

    /**
     * Returns a MapTile[][] of the current Maps tiles
     * @return the maptile matrix of the current maps tiles.
     */
    public MapTile[][] getMapTiles() { return mapTiles; }

    /**
     * Returns a int of the current Maps width in Tiles
     * @return the int of the current maps width in tiles
     */
    public int getMapWidth() { return mapWidth; }

    /**
     * Returns a int of the current Maps height in Tiles
     * @return the int of the current maps height in tiles
     */
    public int getMapHeight() { return mapHeight; }

    /**
     * Returns a String of the associated .dat file to this Map.
     * @return the string of the associated .dat file to this map
     */
    public String getPATH() { return PATH; }

    /**
     * Returns a String of the associated .meta file to this Map.
     * @return the string of the associated .meta file to this map
     */
    public String getMetaPATH() { return metaPATH; }

    /**
     * Returns a String[] of the relative paths to all the TileSets associated to this map.
     * @return the string array of the relative paths to all the tilesets associated to this map
     */
    public String[] getTileSetPaths() {
        String[] tilePaths = new String[tileSets.size()];
        for(int step=0; step<tileSets.size(); step++) {
            tilePaths[step] = tileSets.get(step).getTileSetPath();
        }
        return tilePaths;
    }

    /**
     * Returns a TileSet[] of all the associated TileSets to this map.
     * @return the tileset array of all tilesets associated to this map
     */
    public TileSet[] getTileSets() {
        TileSet[] toReturn = new TileSet[tileSets.size()];
        tileSets.toArray(toReturn);
        return toReturn;
    }

    /**
     * Returns the TileSet associated with this map at index.
     * @param index the tileset to check
     * @return the tileset of this map for index provided
     */
    public TileSet getTileSet(int index) { return tileSets.get(index); }

    /**
     * Will set the Maps mapTiles array to a new MapTile[][] set.
     * @param newMap the new MapTile matrix to set this Maps mapTiles
     */
    public void setMapTiles(MapTile[][] newMap) { mapTiles = newMap; }

    /**
     * Returns the TileType of the MapTile located at x,y.
     * @param x the target x location to check
     * @param y the target y location to check
     * @return the MapTile.TileType of the target tile
     */
    public MapTile.TileType getTileType(int x, int y) { return mapTiles[y][x].getType(); }

    /**
     * Returns an int[] of all the tileIDs on this map that are considered Fire.
     * @return an integer array of all the tileIDs on this map that are considered fire
     */
    public int[] getMapFireTileIDs() {
        int steps = mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).size();
        int[] result = new int[steps];
        for(int i=0; i<steps; i++) {
            result[i] = mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).get(i);
        }
        return result;
    }

    /**
     * Returns an int[] of all the tileIDs on this map that are considered Impassable.
     * @return an integer array of all the tileIDs on this map that are considered impassable
     */
    public int[] getMapImpassableTileIDs() {
        int steps = mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).size();
        int[] result = new int[steps];
        for(int i=0; i<steps; i++) {
            result[i] = mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).get(i);
        }
        return result;
    }

    /**
     * Sets this maps fire tile ids or removes them
     * @param tileID the target tileID to add or remove from the fire list
     * @param remove true will remove the tileID from the list and false will add it
     */
    public void setMapFireTileIDs(int tileID, boolean remove) {
        if(remove) {
            int index = mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).indexOf(tileID);
            mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).remove(index);
        } else {
            mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).add(tileID);
        }
    }

    /**
     * Sets this maps impassable tile ids or removes them.
     * @param tileID the target tileID to add or remove from the impassable list
     * @param remove true will remove the tileID from the list and false will add it
     */
    public void setMapImpassableTileIDs(int tileID, boolean remove) {
        if(remove) {
            int index = mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).indexOf(tileID);
            mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).remove(index);
        } else {
            mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).add(tileID);
        }
    }

    /**
     * Returns the full line of meta data for Enemies information.
     * @return a string that represents the enemies for this map
     */
    public String getMetaEnemies() { return metaEnemies; }

    /**
     * Returns the full line of meta data for Allies information.
     * @return a string that represents the allies for this map
     */
    public String getMetaAllies() { return metaAllies; }

    /**
     * Returns the full line of meta data for Start Location information.
     * @return a string that represents the players start location for this map
     */
    public String getMetaStartLoc() { return metaStartLoc; }

    /**
     * Returns the associated icon image if one exists in the /art folder
     * @return the associated icon image if one exists in the /art folder
     */
    public Image getIcon() { return icon; }

    /**
     * Assigns the maps icon
     * @param newIcon will assign the maps icon
     */
    public void setIcon(Image newIcon) {
        icon = newIcon;
    }

    /**
     * Assigns new mapDataTileMetaIDs useful when switching through maps
     * @param mapDataTileMetaIDs will assign new data for tile type meta data
     */
    public void setMapDataTileMetaIDs(java.util.ArrayList<java.util.ArrayList<Integer>> mapDataTileMetaIDs) {
        this.mapDataTileMetaIDs = mapDataTileMetaIDs;
    }

}

