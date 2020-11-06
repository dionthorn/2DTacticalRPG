package org.dionthorn;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Run will handle the core engine of the game including the JavaFX Application, Setting up Input Handling,
 * updating entities and making calls to Render as well as using gameState States to change the render conditions, etc.
 * All of .render() has been moved to a static Render.render() and some class variables have been put there.
 */
public class Run extends Application {

    public static final String PROGRAM_VERSION = "v0.2.3a";
    public static boolean JRT = false;
    public static final String SYS_LINE_SEP = System.lineSeparator();
    public static Logger programLogger = Logger.getLogger("programLogger");
    public static URI GAME_DATA_PATH;
    public static URI GAME_ART_PATH;
    public static URI GAME_MAP_PATH;
    public static URI GAME_ITEM_PATH;
    public static URI MOD_PATH;
    public static URI MOD_ART_PATH;
    public static URI MOD_MAP_PATH;
    public static URI MOD_ITEM_PATH;
    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;
    public static int SCREEN_MAP_HEIGHT;
    public static final int TILE_SIZE = 32;
    private final int[] DRAG_LOC = {-1, -1};
    private final long FPS = TimeUnit.SECONDS.toNanos(1L) / 60;
    private int lastSelectedCharUID;
    private long startTime = System.nanoTime();
    private GraphicsContext gc;
    private GameState gameState;
    private DevMenu devMenu;

    /**
     * Called after every update()
     * will check gameState and decide what to render from that.
     */
    private void render() { RenderUtil.render(this, devMenu, gc); }

    /**
     * Called before every draw call
     */
    public void update() {
        if(gameState != null && gameState.getCurrentState() == GameState.STATE.GAME) {
            for(Entity e: gameState.getEntities()) {
                if(e instanceof Updateable) {
                    if( ((PhysicalEntity) e).getCurrentMap().equals(gameState.getCurrentMap()) ) {
                        ((Updateable) e).update(gameState);
                    }
                }
            }
        }
    }

    /**
     * Sets up the gameState for a new game environment.
     */
    private void newGame() {

        Entity.GEN_COUNT = 0; // reset entity counter since this should be a 'fresh' game.
        for(String path: FileOpUtils.getFileNamesFromDirectory(GAME_MAP_PATH)) {
            if(!path.equals(".gitattributes") && !path.contains("meta")) {
                if(gameState != null) {
                    gameState.getMaps().add(new Map(GAME_MAP_PATH + (JRT ? "/" + path : path)));
                } else {
                    gameState = new GameState(new Map(GAME_MAP_PATH + (JRT ? "/" + path : path)));
                }
            }
        }

        // Need to also check for /Mod/Maps folder potential maps.
        if(JRT && new File(MOD_MAP_PATH.getPath()).exists()) {
            File testDir = new File(MOD_MAP_PATH.getPath());
            String[] tempList = testDir.list();
            if(testDir.isDirectory() && tempList != null && tempList.length != 0) {
                for(String path: FileOpUtils.getFileNamesFromDirectory(MOD_MAP_PATH)) {
                    if(!path.contains("meta")) {
                        if(gameState != null) {
                            gameState.getMaps().add(new Map(MOD_MAP_PATH + (JRT ? "/" + path : path)));
                            programLogger.log(Level.INFO, "Maps loaded from Mod/Maps");
                        } else {
                            programLogger.log(Level.INFO, "GameState is null can't load MOD Maps");
                        }
                    }
                }
            }
        }
        String[] startLoc = gameState.getCurrentMap().getMetaStartLoc().split(":")[0].split(",");
        String[] allies = gameState.getCurrentMap().getMetaAllies().split(":")[0].split("/");
        String[] enemies = gameState.getCurrentMap().getMetaEnemies().split(":")[0].split("/");
        String[] items = gameState.getCurrentMap().getMetaItems().split(":")[0].split("/");

        // For now a default player entity, in future will have a character_creation state for building the player
        gameState.getEntities().add(
                new Player(
                        gameState.getCurrentMap(),
                        "Characters/MartialClassPlayer.png", "Player",
                        Integer.parseInt(startLoc[0]), Integer.parseInt(startLoc[1]),
                        new MartialClass()
                )
        );
        gameState.getPlayerTeam().add(gameState.getPlayerEntity());

        // setup allies
        CharacterClass tempClass = null;
        for(String ally: allies) {
            if(!ally.equals("")) {
                String[] temp = ally.split(",");
                String name = temp[0];
                int x = Integer.parseInt(temp[1]);
                int y = Integer.parseInt(temp[2]);
                if(temp[3].equals("magic")) {
                    tempClass = new MagicClass();
                } else if(temp[3].equals("martial")) {
                    tempClass = new MartialClass();
                }
                assert tempClass != null;
                NonPlayerCharacter tempChar = new NonPlayerCharacter(gameState.getCurrentMap(),
                        tempClass.getDefaultSpriteAlly(), name, x, y, tempClass
                );
                gameState.getEntities().add(tempChar);
                gameState.getPlayerTeam().add(tempChar);
            }
        }

        // setup enemies
        for(String enemy: enemies) {
            if(!enemy.equals("")) {
                String[] temp = enemy.split(",");
                String name = temp[0];
                int x = Integer.parseInt(temp[1]);
                int y = Integer.parseInt(temp[2]);
                if(temp[3].equals("magic")) {
                    tempClass = new MagicClass();
                } else if(temp[3].equals("martial")) {
                    tempClass = new MartialClass();
                }
                assert tempClass != null;
                NonPlayerCharacter tempChar = new NonPlayerCharacter(gameState.getCurrentMap(),
                        tempClass.getDefaultSpriteEnemy(), name, x, y, tempClass
                );
                gameState.getEntities().add(tempChar);
                gameState.getEnemyTeam().add(tempChar);
            }
        }

        // setup items
        for(String line: items) {
            System.out.println(line);
            // Gold,10,10/:ITEMS
            if(!line.equals("") && !line.contains("//")) {
                String[] values = line.split(",");
                // System.out.println("ADDING ITEM: " + values[1]);
                gameState.getEntities().add(ItemOnMap.makeItemOnMap(values[0], values[1],
                        Integer.parseInt(values[2]), Integer.parseInt(values[3])));
            }
        }
    }


    /**
     * This is the core GUI processing of the program and the initial JavaFX setup point.
     * @param primaryStage the 'stage' or 'window' object to be used by the OS.
     */
    @Override
    public void start(Stage primaryStage) {
        RenderUtil.mainMenuBg = new Image(GAME_ART_PATH + (JRT ? "/main_menu.png" : "main_menu.png"));
        RenderUtil.paperBg = new Image(GAME_ART_PATH + (JRT ? "/paper.png" : "paper.png"),
                Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT-Run.SCREEN_MAP_HEIGHT, false, false
        );
        primaryStage.setTitle("OOP Game Project " + PROGRAM_VERSION);
        primaryStage.setResizable(false);
        Group rootGroup = new Group();
        Scene rootScene = new Scene(rootGroup, SCREEN_WIDTH, SCREEN_HEIGHT, Color.BLACK);
        primaryStage.sizeToScene();
        Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        rootGroup.getChildren().add(canvas);

        // Keyboard handling
        rootScene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            // programLogger.log(Level.INFO, "[DEBUG] KeyInput: " + key.getCode());
            if (gameState == null || gameState.getCurrentState() == GameState.STATE.MAIN_MENU) {
                if (key.getCode() == KeyCode.ESCAPE) {
                    primaryStage.close();
                }
            } else if(gameState.getCurrentState() == GameState.STATE.GAME) {
                if(key.getCode() == KeyCode.ESCAPE) {
                    gameState.setState(GameState.STATE.LEVEL_SELECTION);
                }
                if(key.getCode() == KeyCode.BACK_QUOTE) {
                    String[] folders = getGameState().getCurrentMap().getPATH().split("/");
                    String shortPath = folders[folders.length - 1];
                    if(devMenu == null) {
                        devMenu = new DevMenu(this);
                        devMenu.getDevMapPath().setText(String.format("%s", shortPath));
                        for(Map toLoad: gameState.getMaps()) {
                            folders = toLoad.getPATH().split("/");
                            shortPath = folders[folders.length - 1];
                            devMenu.getMapList().getItems().add(shortPath);
                        }
                        devMenu.getMapList().getSelectionModel().select(0);
                    } else {
                        devMenu.show();
                    }
                }
                if(key.getCode() == KeyCode.UP) {
                    if(gameState.getPlayerEntity().isMoveTurn()) {
                        Character player = gameState.getPlayerEntity();
                        Character enemy = player.checkEnemyCollision(gameState, player.getX(), player.getY() - 1);

                        ItemOnMap item = player.checkItemCollision(gameState, player.getX(), player.getY() - 1);
                        if(item != null) {
                            player.addItem(item);
                            for(Entity e: gameState.getEntities()) {
                                if(e.equals(item)) {
                                    gameState.getEntities().remove(item);
                                    break;
                                }
                            }
                        }

                        if (!player.checkFriendlyCollision(gameState, player.getX(), player.getY() - 1)) {
                            if(enemy != null) {
                                if(enemy.isAlive()) {
                                    gameState.startBattle(player, enemy);
                                }
                            } else {
                                gameState.getPlayerEntity().move(0, -1);
                                gameState.getPlayerEntity().setMoveTurn(false);
                                gameState.nextTurn(gameState.getPlayerTeam().get(1).getUID());
                            }
                        }
                    }
                } else if(key.getCode() == KeyCode.RIGHT) {
                    if(gameState.getPlayerEntity().isMoveTurn()) {
                        Character player = gameState.getPlayerEntity();
                        Character enemy = player.checkEnemyCollision(gameState, player.getX() + 1, player.getY());

                        ItemOnMap item = player.checkItemCollision(gameState, player.getX() + 1, player.getY());
                        if(item != null) {
                            player.addItem(item);
                            for(Entity e: gameState.getEntities()) {
                                if(e.equals(item)) {
                                    gameState.getEntities().remove(item);
                                    break;
                                }
                            }
                        }

                        if (!player.checkFriendlyCollision(gameState, player.getX() + 1, player.getY())) {
                            if(enemy != null) {
                                if(enemy.isAlive()) {
                                    gameState.startBattle(player, enemy);
                                }
                            } else {
                                gameState.getPlayerEntity().move(1, 0);
                                gameState.getPlayerEntity().setMoveTurn(false);
                                gameState.nextTurn(gameState.getPlayerTeam().get(1).getUID());
                            }
                        }
                    }
                } else if(key.getCode() == KeyCode.LEFT) {
                    if(gameState.getPlayerEntity().isMoveTurn()) {
                        Character player = gameState.getPlayerEntity();
                        Character enemy = player.checkEnemyCollision(gameState, player.getX() - 1, player.getY());

                        ItemOnMap item = player.checkItemCollision(gameState, player.getX() - 1, player.getY());
                        if(item != null) {
                            player.addItem(item);
                            for(Entity e: gameState.getEntities()) {
                                if(e.equals(item)) {
                                    gameState.getEntities().remove(item);
                                    break;
                                }
                            }
                        }

                        if (!player.checkFriendlyCollision(gameState, player.getX() - 1, player.getY())) {
                            if(enemy != null) {
                                if(enemy.isAlive()) {
                                    gameState.startBattle(player, enemy);
                                }
                            } else {
                                gameState.getPlayerEntity().move(-1, 0);
                                gameState.getPlayerEntity().setMoveTurn(false);
                                gameState.nextTurn(gameState.getPlayerTeam().get(1).getUID());
                            }
                        }
                    }
                } else if(key.getCode() == KeyCode.DOWN) {
                    if(gameState.getPlayerEntity().isMoveTurn()) {
                        Character player = gameState.getPlayerEntity();
                        Character enemy = player.checkEnemyCollision(gameState, player.getX(), player.getY() + 1);

                        ItemOnMap item = player.checkItemCollision(gameState, player.getX(), player.getY() + 1);
                        if(item != null) {
                            player.addItem(item);
                            for(Entity e: gameState.getEntities()) {
                                if(e.equals(item)) {
                                    gameState.getEntities().remove(item);
                                    break;
                                }
                            }
                        }

                        if (!player.checkFriendlyCollision(gameState, player.getX(), player.getY() + 1)) {
                            if(enemy != null) {
                                if(enemy.isAlive()) {
                                    gameState.startBattle(player, enemy);
                                }
                            } else {
                                gameState.getPlayerEntity().move(0, 1);
                                gameState.getPlayerEntity().setMoveTurn(false);
                                gameState.nextTurn(gameState.getPlayerTeam().get(1).getUID());
                            }
                        }
                    }
                } else if(key.getCode() == KeyCode.SPACE) {
                    if(!gameState.getNextTurn() && !gameState.getPlayerEntity().isMoveTurn()) {
                        gameState.setNextTurn(true);
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.BATTLE) {
                if (key.getCode() == KeyCode.ESCAPE) {
                    gameState.setState(GameState.STATE.GAME);
                } else if (key.getCode() == KeyCode.A) {
                    programLogger.log(Level.INFO, "Name: " + gameState.getAttacker().getName() + SYS_LINE_SEP +
                            "IsAttacking: " + gameState.getAttacker().isAttacking() + SYS_LINE_SEP +
                            "isBattleTurn: " + gameState.getAttacker().isBattleTurn() + SYS_LINE_SEP +
                            "CompletedCycles: " + gameState.getAttacker().getCharClass().getCompletedCycles()
                    );
                } else if (key.getCode() == KeyCode.D) {
                    programLogger.log(Level.INFO, "Name: " + gameState.getDefender().getName() + SYS_LINE_SEP +
                            "IsAttacking: " + gameState.getDefender().isAttacking() + SYS_LINE_SEP +
                            "isBattleTurn: " + gameState.getDefender().isBattleTurn() + SYS_LINE_SEP +
                            "CompletedCycles: " + gameState.getDefender().getCharClass().getCompletedCycles()
                    );
                }
            } else if(gameState.getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
                if(key.getCode() == KeyCode.ENTER) {
                    gameState.setState(GameState.STATE.GAME);
                    gameState.getPlayerEntity().setMoveTurn(true);
                } else if(key.getCode() == KeyCode.ESCAPE) {
                    gameState.setState(GameState.STATE.EXIT_TO_MAIN);
                }
                // If you press escape in level selection mode it should prompt you if you 'really want to quit'.
                // maybe a dedicated screen for this
            } else if(gameState.getCurrentState() == GameState.STATE.GAME_OVER) {
                if(key.getCode() == KeyCode.ESCAPE) {
                    gameState = null;
                }
            } else if(gameState.getCurrentState() == GameState.STATE.EXIT_TO_MAIN) {
                if(key.getCode() == KeyCode.Y) {
                    gameState = null;
                } else if(key.getCode() == KeyCode.N) {
                    gameState.setState(gameState.getPreviousState());
                }
            } else if(gameState.getCurrentState() == GameState.STATE.SETTINGS) {
                if(key.getCode() == KeyCode.DIGIT1) {
                    // need to assign screen settings for 720p screens
                    primaryStage.setWidth(704);
                    primaryStage.setHeight(704);
                    SCREEN_WIDTH = 704;
                    SCREEN_HEIGHT = 704;
                    SCREEN_MAP_HEIGHT = 512;
                } else if(key.getCode() == KeyCode.DIGIT2) {
                    // need to assign screen settings for 1080p screens
                    primaryStage.setWidth(1024);
                    primaryStage.setHeight(1024);
                    SCREEN_WIDTH = 1024;
                    SCREEN_HEIGHT = 1024;
                    SCREEN_MAP_HEIGHT = 768;
                } else if(key.getCode() == KeyCode.ESCAPE) {
                    // check if in JRT file system, if true, check if user_settings exists in mod folder
                    // if not then create one with the current graphics settings.
                    if(JRT) {
                        if(new File(URI.create(MOD_PATH + "/user_settings.txt").getPath()).exists()) {
                            programLogger.log(Level.INFO, "user_settings was found in /Mod");
                        } else {
                            FileOpUtils.createFile(URI.create(MOD_PATH + "/user_settings.txt"));
                        }
                        String[] userSettings = { "SCREEN_WIDTH=" + SCREEN_WIDTH,
                                "SCREEN_HEIGHT=" + SCREEN_HEIGHT,
                                "SCREEN_MAP_HEIGHT=" + SCREEN_MAP_HEIGHT };
                        FileOpUtils.writeFileLines(URI.create(MOD_PATH + "/user_settings.txt"), userSettings);
                        programLogger.log(Level.INFO, "user_settings has been updated to current graphics settings");
                    }
                    gameState.setState(GameState.STATE.MAIN_MENU);
                }
            }
        });

        // Mouse left click handling
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            int mouseX = (int) mouseEvent.getX();
            int mouseY = (int) mouseEvent.getY();
            programLogger.log(Level.INFO, "Mouse Event: (" + mouseX + ", " + mouseY + ")");
            if(gameState != null && devMenu != null && devMenu.EDIT_MODE &&
                    gameState.getCurrentState() == GameState.STATE.GAME) {
                int tileX = (mouseX / gameState.getCurrentMap().getTileSize()) + RenderUtil.anchorUL[0];
                int tileY = (mouseY / gameState.getCurrentMap().getTileSize()) + RenderUtil.anchorUL[1];
                // programLogger.log(Level.INFO, "x,y: " + tileX + " " + tileY);
                if(DRAG_LOC[0] == -1) {
                    if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                        MapTile[][] tempMapTiles = gameState.getCurrentMap().getMapTiles();
                        // programLogger.log(Level.INFO, "TILEID: " + tempMapTiles[tileY][tileX].getTileID());
                        tempMapTiles[tileY][tileX].setTileID(devMenu.SELECTED_TILE_ID);
                        tempMapTiles[tileY][tileX].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                        gameState.getCurrentMap().setMapTiles(tempMapTiles);
                    } else if(mouseEvent.getButton() == MouseButton.SECONDARY) {
                        MapTile[][] tempMapTiles = gameState.getCurrentMap().getMapTiles();
                        // programLogger.log(Level.INFO, "TILEID: " + tempMapTiles[tileY][tileX].getTileID());
                        tempMapTiles[tileY][tileX].setTileID(gameState.getCurrentMap().getTileSet(
                                devMenu.SELECTED_TILE_SET_ID).getTiles().length - 1
                        );
                        tempMapTiles[tileY][tileX].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                        gameState.getCurrentMap().setMapTiles(tempMapTiles);
                    }
                } else {
                    if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                        int releasedX = (int) (mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize()) + RenderUtil.anchorUL[0];
                        int releasedY = (int) (mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize()) + RenderUtil.anchorUL[1];
                        if(releasedX <= DRAG_LOC[0] && releasedY > DRAG_LOC[1]) {
                            for (int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for (int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(devMenu.SELECTED_TILE_ID);
                                    tempMap[y][x].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                                    gameState.getCurrentMap().setMapTiles(tempMap);
                                }
                            }
                        } else if(releasedY <= DRAG_LOC[1] && releasedX > DRAG_LOC[0]) {
                            for(int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for (int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(devMenu.SELECTED_TILE_ID);
                                    tempMap[y][x].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                                    gameState.getCurrentMap().setMapTiles(tempMap);
                                }
                            }
                        } else if(releasedX <= DRAG_LOC[0]) {
                            for(int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for(int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(devMenu.SELECTED_TILE_ID);
                                    tempMap[y][x].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                                    gameState.getCurrentMap().setMapTiles(tempMap);
                                }
                            }
                        } else {
                            for(int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for(int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(devMenu.SELECTED_TILE_ID);
                                    tempMap[y][x].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                                    gameState.getCurrentMap().setMapTiles(tempMap);
                                }
                            }
                        }
                    } else if(mouseEvent.getButton() == MouseButton.SECONDARY) {
                        int releasedX = (int) (mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize()) + RenderUtil.anchorUL[0];
                        int releasedY = (int) (mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize()) + RenderUtil.anchorUL[1];
                        if(releasedX <= DRAG_LOC[0] && releasedY > DRAG_LOC[1]) {
                            for(int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for(int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(
                                            devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1
                                    );
                                }
                            }
                        } else if(releasedY <= DRAG_LOC[1] && releasedX > DRAG_LOC[0]) {
                            for(int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for(int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(
                                            devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1
                                    );
                                }
                            }
                        } else if(releasedX <= DRAG_LOC[0]) {
                            for(int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for(int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(
                                            devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1
                                    );
                                }
                            }
                        } else {
                            for(int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for(int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(
                                            devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1
                                    );
                                }
                            }
                        }
                    }
                    DRAG_LOC[0] = -1;
                    DRAG_LOC[1] = -1;
                }
            } else if(gameState == null || gameState.getCurrentState() == GameState.STATE.MAIN_MENU) {
                int xLoc = RenderUtil.menuNewGameBounds[0];
                int yLoc = RenderUtil.menuNewGameBounds[1];
                int xWidth = RenderUtil.menuNewGameBounds[2];
                int yHeight = RenderUtil.menuNewGameBounds[3];
                if(mouseX >= xLoc && mouseX <= xWidth + xLoc) {
                    if(mouseY <= yLoc && mouseY >= yLoc - yHeight) {
                        gameState = null;
                        newGame();
                        gameState.setState(GameState.STATE.LEVEL_SELECTION);
                    }
                }
                xLoc = RenderUtil.menuSettingsBounds[0];
                yLoc = RenderUtil.menuSettingsBounds[1];
                xWidth = RenderUtil.menuSettingsBounds[2];
                yHeight = RenderUtil.menuSettingsBounds[3];
                if(mouseX >= xLoc && mouseX <= xWidth + xLoc) {
                    if(mouseY <= yLoc && mouseY >= yLoc - yHeight) {
                        if(gameState == null) {
                            newGame();
                        }
                        gameState.setState(GameState.STATE.SETTINGS);
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.GAME) {
                int[] tileXY = { mouseX / TILE_SIZE, mouseY / TILE_SIZE };
                for(Entity e: gameState.getEntities()) {
                    if(e instanceof Character) {
                        int charX = ((Character) e).getRelativeX();
                        int charY = ((Character) e).getRelativeY();
                        if(charX == tileXY[0] && charY == tileXY[1]) {
                            lastSelectedCharUID = e.getUID();
                            programLogger.log(Level.INFO, "A Character was Clicked" + SYS_LINE_SEP +
                                    "getUID returns: " + e.getUID() + SYS_LINE_SEP +
                                    "getName returns: " + ((Character) e).getName() + SYS_LINE_SEP +
                                    "isMoveTurn returns: " + ((Character) e).isMoveTurn() + SYS_LINE_SEP +
                                    "isAlive returns: " + ((Character) e).isAlive() + SYS_LINE_SEP +
                                    "getHp returns: " + ((Character)e).getHp() + SYS_LINE_SEP +
                                    "getAttack returns: " + ((Character)e).getAttack() + SYS_LINE_SEP +
                                    "getCritical returns: " + ((Character)e).getCritical() + SYS_LINE_SEP +
                                    "getDefense returns: " + ((Character)e).getDefense() + SYS_LINE_SEP +
                                    "getCharClass.getLevel returns: " +
                                    ((Character) e).getCharClass().getLevel() + SYS_LINE_SEP +
                                    "getCharClass.getCurrentXP returns: " +
                                    ((Character) e).getCharClass().getCurrentXP()
                            );
                        }
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.BATTLE) {
                final int defendX = (SCREEN_WIDTH>>1)+(SCREEN_WIDTH>>4);
                final int defendY = (SCREEN_MAP_HEIGHT>>1)+(SCREEN_MAP_HEIGHT>>3)+(SCREEN_MAP_HEIGHT>>4);
                final int defendW = (SCREEN_WIDTH>>2)+(SCREEN_WIDTH>>3);
                final int defendH = SCREEN_MAP_HEIGHT>>2;
                if((mouseX >= defendX && mouseX <= defendX + defendW) &&
                        (mouseY >= defendY && mouseY <= defendY + defendH))
                {
                    programLogger.log(Level.INFO, "Defend Button was Clicked");
                }
                final int attackX = SCREEN_WIDTH>>4;
                final int attackY = defendY;
                final int attackW = (SCREEN_WIDTH>>2)+(SCREEN_WIDTH>>3);
                final int attackH = SCREEN_MAP_HEIGHT>>2;
                if((mouseX >= attackX && mouseX <= attackX + attackW) &&
                        (mouseY >= attackY && mouseY <= mouseY + attackH))
                {
                    if(gameState.getPlayerEntity().isBattleTurn()) {
                        gameState.getPlayerEntity().setIsAttacking(true);
                    }
                    programLogger.log(Level.INFO, "Attack Button was Clicked");
                }
            } else if(gameState.getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
                int squareSize;
                if(Run.SCREEN_MAP_HEIGHT < 768) {
                    squareSize = 100;
                } else {
                    squareSize = 200;
                }
                int[][] squareXY = {
                        {(SCREEN_WIDTH>>4),         SCREEN_HEIGHT>>5},
                        {(SCREEN_WIDTH>>4) * 6,     SCREEN_HEIGHT>>5},
                        {(SCREEN_WIDTH>>4) * 11,    SCREEN_HEIGHT>>5},
                        {(SCREEN_WIDTH>>4),         (SCREEN_HEIGHT>>5) * 9},
                        {(SCREEN_WIDTH>>4) * 6,     (SCREEN_HEIGHT>>5) * 9},
                        {(SCREEN_WIDTH>>4) * 11,    (SCREEN_HEIGHT>>5) * 9},
                        {(SCREEN_WIDTH>>4),         (SCREEN_HEIGHT>>5) * 17},
                        {(SCREEN_WIDTH>>4) * 6,     (SCREEN_HEIGHT>>5) * 17},
                        {(SCREEN_WIDTH>>4) * 11,    (SCREEN_HEIGHT>>5) * 17}
                };
                int count = 0;
                for(int[] xy: squareXY) {
                    if((mouseX >= xy[0] && mouseX <= xy[0] + squareSize) &&
                            (mouseY >= xy[1] && mouseY <= xy[1] + squareSize))
                    {
                        if(count < gameState.getMaps().size()) {
                            if(gameState.getCurrentMap().getPATH().equals(gameState.getMaps().get(count).getPATH())) {
                                programLogger.log(Level.INFO, "Map already loaded...");
                            } else {
                                gameState.setCurrentMap(gameState.getMaps().get(count));
                            }
                        }
                    }
                    count++;
                }
            }
        });

        // Mouse drag click handling
        canvas.addEventHandler(MouseDragEvent.DRAG_DETECTED, (mouseEvent) -> {
            if(gameState != null && devMenu != null && devMenu.EDIT_MODE && gameState.getCurrentState() == GameState.STATE.GAME) {
                DRAG_LOC[0] = (int) (mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize()) + RenderUtil.anchorUL[0];
                DRAG_LOC[1] = (int) (mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize()) + RenderUtil.anchorUL[1];
            }
        });

        // Staging and animator setup
        primaryStage.setScene(rootScene);
        primaryStage.show();
        AnimationTimer animator = new AnimationTimer() {
            @Override
            public void handle(long arg0) {
                long currentTime = System.nanoTime();
                if (FPS <= (currentTime - startTime)) {
                    update();
                    render();
                    startTime = currentTime;
                }
            }
        };
        animator.start();
    }

    // Getters and Setters
    /**
     * Return the int array [width, height] in tiles of the current map area
     * @return the int array [width, height] in tiles of the current map area
     */
    public static int[] getMapAreaDimensions() {
        int[] result = new int[2];
        result[0] = SCREEN_WIDTH/TILE_SIZE;
        result[1] = SCREEN_MAP_HEIGHT/TILE_SIZE;
        return result;
    }

    /**
     * Returns the current game state.
     * @return the current game state
     */
    public GameState getGameState() { return gameState; }

    /**
     * Returns the last selected characters unique id.
     * @return the last selected characters unique id
     */
    public int getLastSelectChar () { return lastSelectedCharUID;}

    /**
     * Returns the Canvas object that the GraphicsContext is using
     * @return the Canvas object that the graphics context is using
     */
    public Canvas getCanvas() { return gc.getCanvas(); }

    /**
     * The entry point for the program. We determine where /GameData/ folder is here.
     * As well as any other pre launch variables we should set like screen configuration.
     * @param args command line options - unused.
     */
    public static void main(String[] args) {
        // Check for file system setup.
        String gameDatPath = "GameData/config.txt";
        URL resource = Run.class.getClassLoader().getResource(gameDatPath);
        if(resource == null) {
            // This means we must use JRT URIs for the JLink image
            JRT = true;
            URI uri = URI.create("jrt:/MavenTactical/GameData/");
            Path path = Path.of(uri);
            assert(Files.exists(path));
            FileSystem jrtfs = FileSystems.getFileSystem(URI.create("jrt:/"));
            assert(Files.exists(jrtfs.getPath(path.toString())));
            GAME_DATA_PATH = uri;
            GAME_ART_PATH = URI.create(GAME_DATA_PATH + "Art");
            GAME_MAP_PATH = URI.create(GAME_DATA_PATH + "Maps");
            GAME_ITEM_PATH = URI.create(GAME_DATA_PATH + "Items");
        } else {
            // This means we are using an IDE to run, so we can use Paths.get to find the out folders.
            // in my Intellij setup this is target/classes folder
            GAME_DATA_PATH = Paths.get("target", "classes", "GameData").toAbsolutePath().toUri();
            GAME_ART_PATH = Paths.get("target", "classes", "GameData", "Art").toAbsolutePath().toUri();
            GAME_MAP_PATH = Paths.get("target", "classes", "GameData", "Maps").toAbsolutePath().toUri();
            GAME_ITEM_PATH = Paths.get("target", "classes", "GameData", "Items").toAbsolutePath().toUri();
        }

        // The Mod folder will not use jrt as it should be easily accessible for end users.
        // in JLink images /Mod/ will be found in the /bin folder.
        if(JRT) {
            MOD_PATH = Paths.get("", "Mod").toAbsolutePath().toUri();
            boolean testDir;
            if(new File(MOD_PATH).exists()) {
                programLogger.log(Level.INFO,"Mod folder found at: " + MOD_PATH);
            } else {
                testDir = new File(MOD_PATH).mkdir();
                if(!testDir) {
                    programLogger.log(Level.INFO,"Failed to Create Mod Directory!");
                } else {
                    programLogger.log(Level.INFO,"Mod folder created here: " + MOD_PATH);
                }
            }
            MOD_ART_PATH = URI.create(MOD_PATH + "/Art");
            if(new File(MOD_ART_PATH.getPath()).exists()) {
                programLogger.log(Level.INFO,"Mod/Art folder found at: " + MOD_ART_PATH);
            } else {
                testDir = new File(MOD_ART_PATH.getPath()).mkdir();
                if(!testDir) {
                    programLogger.log(Level.INFO, "Failed to Create Mod/Art Directory!");
                } else {
                    programLogger.log(Level.INFO,"Mod/Art folder will go here: " + MOD_ART_PATH);
                }
            }
            // Want to add the Art_Attributions.txt/Outside_Art_Originals folder
            // to this as well as the Characters/Items/Maps subfolders
            testDir = new File(URI.create(MOD_ART_PATH + "/Characters").getPath()).mkdir();
            if(!testDir) {
                programLogger.log(Level.INFO, "Failed to Create Mod/Art/Characters Directory!");
            } else {
                programLogger.log(Level.INFO,"Mod/Art/Characters folder will go here: " + MOD_ART_PATH);
            }
            testDir = new File(URI.create(MOD_ART_PATH + "/Items").getPath()).mkdir();
            if(!testDir) {
                programLogger.log(Level.INFO, "Failed to Create Mod/Art/Items Directory!");
            } else {
                programLogger.log(Level.INFO,"Mod/Art/Items folder will go here: " + MOD_ART_PATH);
            }
            testDir = new File(URI.create(MOD_ART_PATH + "/Maps").getPath()).mkdir();
            if(!testDir) {
                programLogger.log(Level.INFO, "Failed to Create Mod/Art/Maps Directory!");
            } else {
                programLogger.log(Level.INFO,"Mod/Art/Maps folder will go here: " + MOD_ART_PATH);
            }
            //
            MOD_MAP_PATH = URI.create(MOD_PATH + "/Maps");
            if(new File(MOD_MAP_PATH.getPath()).exists()) {
                programLogger.log(Level.INFO,"Mod/Maps folder found at: " + MOD_MAP_PATH);
            } else {
                testDir = new File(MOD_MAP_PATH.getPath()).mkdir();
                if(!testDir) {
                    programLogger.log(Level.INFO, "Failed to Create Mod/Maps Directory!");
                } else {
                    programLogger.log(Level.INFO,"Mod/Maps folder will go here: " + MOD_ART_PATH);
                }
            }
            MOD_ITEM_PATH = URI.create(MOD_PATH + "/Items");
            if(new File(MOD_ITEM_PATH.getPath()).exists()) {
                programLogger.log(Level.INFO, "Mod/Items folder found at: " + MOD_ITEM_PATH);
            } else {
                testDir = new File(MOD_ITEM_PATH.getPath()).mkdir();
                if(!testDir) {
                    programLogger.log(Level.INFO, "Failed to Create Mod/Items Directory!");
                } else {
                    programLogger.log(Level.INFO, "Mod/Items folder will go here: " + MOD_ITEM_PATH);
                }
            }
        }

        // Need to check if /Mod/ has a user_settings.txt
        // if not then use the internal jrt config.txt default settings
        URI config;
        if(JRT && new File(URI.create(MOD_PATH + "user_settings.txt").getPath()).exists()) {
            programLogger.log(Level.INFO, "user_settings was found in /Mod");
            config = URI.create(MOD_PATH + "user_settings.txt");
        } else {
            programLogger.log(Level.INFO, "config default settings will be used");
            config = URI.create(GAME_DATA_PATH + "config.txt");
        }

        String[] startUpSettings = FileOpUtils.getFileLines(config);
        for(String line: startUpSettings) {
            if(line.contains("SCREEN_WIDTH")) {
                SCREEN_WIDTH = Integer.parseInt(line.split("=")[1]);
            } else if(line.contains("SCREEN_HEIGHT")) {
                SCREEN_HEIGHT = Integer.parseInt(line.split("=")[1]);
            } else if(line.contains("SCREEN_MAP_HEIGHT")) {
                SCREEN_MAP_HEIGHT = Integer.parseInt(line.split("=")[1]);
            }
        }
        // Launch the JavaFX Application this will take us to @Override public void start(Stage primaryStage)
        launch(args);
    }

}