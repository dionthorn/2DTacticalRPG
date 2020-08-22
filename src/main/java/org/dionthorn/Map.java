package org.dionthorn;

import javafx.scene.image.Image;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

// Pre-Complied Maps Should be located in /GameData/Maps

public class Map {
    // Map class is the in memory representation of a game map
    private static int GEN_COUNT = 0; // will only be used in beta for more maps
    // Used to determine metaData
    private final int WIDTH_DATA = 0; // Width in tiles
    private final int HEIGHT_DATA = 1; // Height in tiles
    private final int TILE_DATA = 2; // Tile Size in pixels
    private final int IMG_SRC_DATA = 3; // tile set paths should be third entry and all past that.
    // Tile meta data should be a file named the exact same as .dat but named .meta
    private String PATH; // Relative path to this maps .dat file
    private String metaPATH;
    private String metaEnemies;
    private String metaAllies;
    private String metaStartLoc;
    private int TILE_SIZE = 32; // Tile size in pixels could be per map basis though
    private MapTile[][] mapTiles;
    private int mapWidth; // Unit: Tiles
    private int mapHeight;
    private ArrayList<ArrayList<Integer>> mapDataTileMetaIDs = new ArrayList<ArrayList<Integer>>();
    // List of tileSets used by map (maybe for Beta enable use of multiple tileSets per map)
    private ArrayList<TileSet> tileSets = new ArrayList<>();

    public Map(String datPath) {
        // Load a map from a file
        GEN_COUNT++;
        PATH = datPath;
        String[] tempStr = datPath.split("\\.");
        // process meta data
        metaPATH = tempStr[0] + ".meta";
        String[] tileMetaData = getFileLines(metaPATH);
        for(int i=0; i< MapTile.TileType.values().length; i++) {
            mapDataTileMetaIDs.add(new ArrayList<Integer>());
        }
        for(String line: tileMetaData) {
            if(line.contains("FIRE")) {
                String[] tileIdsFire = line.split(":")[0].split(",");
                for(int i=0; i<tileIdsFire.length; i++) {
                    if(!tileIdsFire[i].equals("")) {
                        int tempInt = Integer.parseInt(tileIdsFire[i]);
                        mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).add(tempInt);
                    }
                }
            } else if(line.contains("IMPASSABLE")) {
                String[] tileIdsImpassable = tileMetaData[1].split(":")[0].split(",");
                for(int i=0; i<tileIdsImpassable.length; i++) {
                    if(!tileIdsImpassable[i].equals("")) {
                        int tempInt = Integer.parseInt(tileIdsImpassable[i]);
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
        // process tile data
        boolean first = true;
        String[] data = getFileLines(PATH);
        int xCount = 0;
        int yCount = 0;
        for(String line: data) {
            if(first) {
                // The first line of file must contain metadata about the map: width/height in tiles
                // tileSetID:TileID for each tile, and finally the tile size in pixels.
                String[] splitLine = line.replaceAll(" ", "").split(",");
                mapWidth = Integer.parseInt(splitLine[WIDTH_DATA]);
                mapHeight = Integer.parseInt(splitLine[HEIGHT_DATA]);
                mapTiles = new MapTile[mapHeight][mapWidth];
                TILE_SIZE = Integer.parseInt(splitLine[TILE_DATA]);
                // For each space after this we should have tileSet paths
                for(int step=IMG_SRC_DATA; step<splitLine.length; step++) {
                    tileSets.add(new TileSet(splitLine[step], TILE_SIZE));
                }
                first = false;
            } else {
                // This is the parsing of tileSetIDs and tileIDs
                String[] splitLine = line.replaceAll(" ", "").split(",");
                for(String toProcess: splitLine) {
                    String[] finalSplit = toProcess.split(":");
                    // tileSetID:tileID
                    mapTiles[yCount][xCount] = new MapTile(Integer.parseInt(finalSplit[0]),Integer.parseInt(finalSplit[1]));
                    //Temporary parsing for metadata
                    boolean cont = true;
                    for(int x = 0; x <mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).size(); x++) {
                        if(mapTiles[yCount][xCount].getTileID() == mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).get(x)) {
                            mapTiles[yCount][xCount].tagFire();
                            cont = false;
                        }
                    }
                    if(cont) {
                        for(int x = 0; x <mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).size(); x++) {
                            if(mapTiles[yCount][xCount].getTileID()==mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).get(x)) {
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

    public Map(String[] tilePaths) {
        // Random map generation based on the provided tilePaths
        GEN_COUNT++;
        Random rand = new Random();
        PATH = String.format("src/main/java/resources/GameData/Maps/RANDOM%d.dat", GEN_COUNT);
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
    }


    // Getters and Setters
    public Image getTile(int setIndex, int tileID) {
        // Access the Image of a specific tileSetID/tileID
        if(setIndex > tileSets.size() - 1) {
            return tileSets.get(0).getBlank();
        } else if(tileID > tileSets.get(setIndex).getTiles().length - 1) {
            return tileSets.get(setIndex).getBlank();
        } else {
            return tileSets.get(setIndex).getTile(tileID);
        }
    }

    public int getTileSize() { return TILE_SIZE; }

    public MapTile[][] getMapTiles() { return mapTiles; }

    public int getMapWidth() { return mapWidth; }

    public int getMapHeight() { return mapHeight; }

    public String getPATH() { return PATH; }

    public String getMetaPATH() { return metaPATH; }

    public String[] getTileSetPaths() {
        String[] tilePaths = new String[tileSets.size()];
        for(int step=0; step<tileSets.size(); step++) {
            tilePaths[step] = tileSets.get(step).getTileSetPath();
        }
        return tilePaths;
    }

    public TileSet[] getTileSets() {
        TileSet[] toReturn = new TileSet[tileSets.size()];
        tileSets.toArray(toReturn);
        return toReturn;
    }

    public TileSet getTileSet(int index) { return tileSets.get(index); }

    public void setMapTiles(MapTile[][] newMap) { mapTiles = newMap; }

    public MapTile.TileType getTileType(int x, int y) { return mapTiles[y][x].getType(); }

    public int[] getMapFireTileIDs() {
        int steps = mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).size();
        int[] result = new int[steps];
        for(int i=0; i<steps; i++) {
            result[i] = mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).get(i);
        }
        return result;
    }

    public int[] getMapImpassableTileIDs() {
        int steps = mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).size();
        int[] result = new int[steps];
        for(int i=0; i<steps; i++) {
            result[i] = mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).get(i);
        }
        return result;
    }

    public void setMapFireTileIDs(int tileID, boolean remove) {
        if(remove) {
            int index = mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).indexOf(tileID);
            mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).remove(index);
        } else {
            mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).add(tileID);
        }
    }

    public void setMapImpassableTileIDs(int tileID, boolean remove) {
        if(remove) {
            int index = mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).indexOf(tileID);
            mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).remove(index);
        } else {
            mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).add(tileID);
        }
    }

    public String getMetaEnemies() {
        return metaEnemies;
    }

    public String getMetaAllies() {
        return metaAllies;
    }

    public String getMetaStartLoc() {
        return metaStartLoc;
    }

    // File Operation Static methods
    public static boolean doesFileExist(String path) {
        boolean answer = false;
        try {
            answer = new File(path).isFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return answer;
    }

    public static String[] getFileLines(String path) {
        // Will return null if it fails or file doesn't exist
        String[] toReturn = null;
        if(doesFileExist(path)) {
            File targetFile = new File(path);
            try {
                byte[] fileByteData = Files.readAllBytes(targetFile.toPath());
                toReturn = new String(fileByteData).split(System.lineSeparator());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Pretty serious file structure problem if this happens
            System.err.println(String.format("File: %s Doesn't Exist!", path));
        }
        return toReturn;
    }

    public static void writeFileLines(String path, String[] data) {
        // Will either write over file from beginning or create a new file
        if(doesFileExist(path)) {
            // File already exists so we will write over it
        } else {
            // File doesn't already exist so we will create a new one
            System.out.println(String.format("File: %s Doesn't Exist Creating New File!", path));
            createFile(path);
        }
        File targetFile = new File(path);
        try {
            // We must take the input String[] and convert each index into a byte[]
            // of the string + a system dependant line separator
            ByteArrayOutputStream convertToBytes = new ByteArrayOutputStream();
            FileOutputStream fileWriter = new FileOutputStream(targetFile);
            for(String s: data) {
                s += System.lineSeparator();
                convertToBytes.write(s.getBytes());
            }
            fileWriter.write(convertToBytes.toByteArray());
            System.out.println(String.format("File: %s Successfully Wrote Data", path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createFile(String path) {
        // will either create a new file or inform that the file already exists
        File file = new File(path);
        try {
            if(file.createNewFile()) {
                System.out.println(String.format("File: %s Has Been Created!", path));
            } else {
                System.out.println(String.format("File: %s Already Exists!", path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveData() {
        // Formats the current maps information into a text editable .dat file
        // The map constructor and this method must use the same serial/deserial system
        String[] dataAsString = new String[mapHeight + 1];
        String formattedPaths = "";
        for(String path: getTileSetPaths()) {
            formattedPaths += path + ", ";
        }
        dataAsString[0] = String.format("%d, %d, %d, %s", mapWidth, mapHeight, TILE_SIZE, formattedPaths);
        // First Line Must be Width/Height/TileSize/TileSetPaths metadata
        // TileSetPaths can be as long as needed
        int yCount = 0;
        int xCount = 0;
        int lineCount = 1;
        StringBuilder temp = new StringBuilder();
        for(int i=0; i<mapWidth*mapHeight; i++) {
            int tileID = mapTiles[yCount][xCount].getTileID();
            int tileSetID = mapTiles[yCount][xCount].getTileSet();
            temp.append(String.format("%d:%d, ", tileSetID, tileID)); // "%d:%d, ", tileSetID, tileID
            xCount++;
            if(xCount == mapWidth) {
                dataAsString[lineCount] = temp.toString();
                temp = new StringBuilder();
                lineCount++;
                xCount = 0;
                yCount++;
            }
        }
        // save .dat file
        writeFileLines(PATH, dataAsString);

        // Next make the .meta file
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
        // make sure to write the allies/enemies too
        dataAsString[2] = metaEnemies;
        dataAsString[3] = metaAllies;
        // Need to add STARTLOC meta data
        dataAsString[4] += metaStartLoc;
        // Save map .meta
        writeFileLines(metaPATH, dataAsString);
    }

}

