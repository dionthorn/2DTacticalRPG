package org.dionthorn;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Run will handle the core engine of the game including the JavaFX Application, Setting up Input Handling,
 * updating entities and making calls to Render as well as using gameState States to change the render conditions, etc.
 * All of .render() has been moved to a static Render.render() a some class variables have been put there
 */
public class Run extends Application {

    public static final String PROGRAM_VERSION = "v0.2.2a";
    public static Logger programLogger = Logger.getLogger("programLogger");
    public static String GAME_DATA_PATH = "";
    public static String GAME_ART_PATH = "";
    public static String GAME_MAP_PATH = "";
    public static int SCREEN_WIDTH = 1024;
    public static int SCREEN_HEIGHT = 1024;
    public static int SCREEN_MAP_HEIGHT = 768;
    public static final int TILE_SIZE = 32;
    private final int[] DRAG_LOC = {-1, -1};
    private final long FPS = TimeUnit.SECONDS.toNanos(1L) / 60;
    private int lastSelectedCharUID;
    private long startTime = System.nanoTime();
    private long currentTime;
    private GraphicsContext gc;
    private GameState gameState;
    private DevMenu devMenu;

    /**
     * Called after every update()
     * will check gameState and decide what to render from that.
     */
    private void render() {
        Render.render(this, devMenu, gc);
    }

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
        for(String path: FileOps.getFileNamesFromDirectory(GAME_MAP_PATH + File.separator)) {
            if(!path.equals("config.dat") && !path.equals(".gitattributes") && !path.contains("meta")) {
                if(gameState != null) {
                    gameState.getMaps().add(new Map(GAME_MAP_PATH + File.separator + path));
                } else {
                    gameState = new GameState(new Map(GAME_MAP_PATH + File.separator + path));
                }
            }
        }
        String[] startLoc = gameState.getCurrentMap().getMetaStartLoc().split(":")[0].split(",");
        String[] allies = gameState.getCurrentMap().getMetaAllies().split(":")[0].split("/");
        String[] enemies = gameState.getCurrentMap().getMetaEnemies().split(":")[0].split("/");
        gameState.getEntities().add(
                new Player(
                        gameState.getCurrentMap(),
                        "MartialClassPlayer.png", "Player",
                        Integer.parseInt(startLoc[0]), Integer.parseInt(startLoc[1]),
                        new MartialClass()
                )
        );
        gameState.getPlayerTeam().add(gameState.getPlayerEntity());
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
        gameState.setState(GameState.STATE.LEVEL_SELECTION);
    }


    /**
     * This is the core GUI processing of the program and the initial JavaFX setup point.
     * @param primaryStage the 'stage' or 'window' object to be used by the OS.
     */
    @Override
    public void start(Stage primaryStage) {
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
                    System.exit(0);
                }
            } else if(gameState.getCurrentState() == GameState.STATE.GAME) {
                if(key.getCode() == KeyCode.ESCAPE) {
                    gameState = null;
                }
                if(key.getCode() == KeyCode.BACK_QUOTE) {
                    String[] folders = getGameState().getCurrentMap().getPATH().split("\\\\");
                    String shortPath = folders[folders.length - 1];
                    if(devMenu == null) {
                        devMenu = new DevMenu(this);
                        devMenu.getDevMapPath().setText(String.format("%s", shortPath));
                        for(Map toLoad: gameState.getMaps()) {
                            folders = toLoad.getPATH().split("\\\\");
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
                    programLogger.log(Level.INFO, "Name: " + gameState.getAttacker().getName() + "\n" +
                            "IsAttacking: " + gameState.getAttacker().IsAttacking() + "\n" +
                            "isBattleTurn: " + gameState.getAttacker().isBattleTurn() + "\n" +
                            "CompletedCycles: " + gameState.getAttacker().getCharClass().getCompletedCycles()
                    );
                } else if (key.getCode() == KeyCode.D) {
                    programLogger.log(Level.INFO, "Name: " + gameState.getDefender().getName() + "\n" +
                            "IsAttacking: " + gameState.getDefender().IsAttacking() + "\n" +
                            "isBattleTurn: " + gameState.getDefender().isBattleTurn() + "\n" +
                            "CompletedCycles: " + gameState.getDefender().getCharClass().getCompletedCycles()
                    );
                }
            } else if(gameState.getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
                if(key.getCode() == KeyCode.ENTER) {
                    gameState.setState(GameState.STATE.GAME);
                    gameState.getPlayerEntity().setMoveTurn(true);
                }
            } else if(gameState.getCurrentState() == GameState.STATE.GAME_OVER) {
                if(key.getCode() == KeyCode.ESCAPE) {
                    gameState = null;
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
                int tileX = (mouseX / gameState.getCurrentMap().getTileSize()) + Render.anchorUL[0];
                int tileY = (mouseY / gameState.getCurrentMap().getTileSize()) + Render.anchorUL[1];
                programLogger.log(Level.INFO, "x,y: " + tileX + " " + tileY);
                if (DRAG_LOC[0] == -1) {
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        MapTile[][] tempMapTiles = gameState.getCurrentMap().getMapTiles();
                        programLogger.log(Level.INFO, "TILEID: " + tempMapTiles[tileY][tileX].getTileID());
                        tempMapTiles[tileY][tileX].setTileID(devMenu.SELECTED_TILE_ID);
                        tempMapTiles[tileY][tileX].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                        gameState.getCurrentMap().setMapTiles(tempMapTiles);
                    } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                        MapTile[][] tempMapTiles = gameState.getCurrentMap().getMapTiles();
                        programLogger.log(Level.INFO, "TILEID: " + tempMapTiles[tileY][tileX].getTileID());
                        tempMapTiles[tileY][tileX].setTileID(gameState.getCurrentMap().getTileSet(
                                devMenu.SELECTED_TILE_SET_ID).getTiles().length - 1
                        );
                        tempMapTiles[tileY][tileX].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                        gameState.getCurrentMap().setMapTiles(tempMapTiles);
                    }
                } else {
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        int releasedX = (int) (mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize()) + Render.anchorUL[0];
                        int releasedY = (int) (mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize()) + Render.anchorUL[1];
                        if (releasedX <= DRAG_LOC[0] && !(releasedY <= DRAG_LOC[1])) {
                            for (int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for (int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(devMenu.SELECTED_TILE_ID);
                                    tempMap[y][x].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                                    gameState.getCurrentMap().setMapTiles(tempMap);
                                }
                            }
                        } else if (releasedY <= DRAG_LOC[1] && !(releasedX <= DRAG_LOC[0])) {
                            for (int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for (int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(devMenu.SELECTED_TILE_ID);
                                    tempMap[y][x].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                                    gameState.getCurrentMap().setMapTiles(tempMap);
                                }
                            }
                        } else if (releasedX <= DRAG_LOC[0]) {
                            for (int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for (int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(devMenu.SELECTED_TILE_ID);
                                    tempMap[y][x].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                                    gameState.getCurrentMap().setMapTiles(tempMap);
                                }
                            }
                        } else {
                            for (int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for (int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(devMenu.SELECTED_TILE_ID);
                                    tempMap[y][x].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                                    gameState.getCurrentMap().setMapTiles(tempMap);
                                }
                            }
                        }
                    } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                        int releasedX = (int) (mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize()) + Render.anchorUL[0];
                        int releasedY = (int) (mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize()) + Render.anchorUL[1];
                        if (releasedX <= DRAG_LOC[0] && !(releasedY <= DRAG_LOC[1])) {
                            for (int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for (int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(
                                            devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1
                                    );
                                }
                            }
                        } else if (releasedY <= DRAG_LOC[1] && !(releasedX <= DRAG_LOC[0])) {
                            for (int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for (int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(
                                            devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1
                                    );
                                }
                            }
                        } else if (releasedX <= DRAG_LOC[0]) {
                            for (int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for (int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(
                                            devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1
                                    );
                                }
                            }
                        } else {
                            for (int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for (int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
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
                int xLoc = Render.menuNewGameBounds[0];
                int yLoc = Render.menuNewGameBounds[1];
                int xWidth = Render.menuNewGameBounds[2];
                int yHeight = Render.menuNewGameBounds[3];
                if(mouseX >= xLoc && mouseX <= xWidth + xLoc) {
                    if(mouseY <= yLoc && mouseY >= yLoc - yHeight) {
                        newGame();
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.GAME) {
                int[] tileXY = { mouseX / TILE_SIZE, mouseY / TILE_SIZE };
                for(Entity e: gameState.getEntities()) {
                    if(e instanceof Character) {
                        int charX = ((Character) e).getX();
                        int charY = ((Character) e).getY();
                        if(charX == tileXY[0] && charY == tileXY[1]) {
                            lastSelectedCharUID = e.getUID();
                            programLogger.log(Level.INFO, "A Character was Clicked\n" +
                                    "getName returns: " + ((Character) e).getName() + "\n" +
                                    "isMoveTurn returns: " + ((Character) e).isMoveTurn() + "\n" +
                                    "isAlive returns: " + ((Character) e).isAlive() + "\n" +
                                    "getHp returns: " + ((Character)e).getHp() + "\n" +
                                    "getAttack returns: " + ((Character)e).getAttack() + "\n" +
                                    "getCritical returns: " + ((Character)e).getCritical() + "\n" +
                                    "getDefense returns: " + ((Character)e).getDefense() + "\n" +
                                    "getCharClass.getLevel returns: " +
                                    ((Character) e).getCharClass().getLevel() + "\n" +
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
                if(mouseX >= defendX && mouseX <= defendX + defendW) {
                    if(mouseY >= defendY && mouseY <= defendY + defendH) {
                        programLogger.log(Level.INFO, "Defend Button was Clicked");
                    }
                }
                final int attackX = SCREEN_WIDTH>>4;
                final int attackY = defendY;
                final int attackW = (SCREEN_WIDTH>>2)+(SCREEN_WIDTH>>3);
                final int attackH = SCREEN_MAP_HEIGHT>>2;
                if(mouseX >= attackX && mouseX <= attackX + attackW) {
                    if(mouseY >= attackY && mouseY <= mouseY + attackH) {
                        if(gameState.getPlayerEntity().isBattleTurn()) {
                            gameState.getPlayerEntity().setIsAttacking(true);
                        }
                        programLogger.log(Level.INFO, "Attack Button was Clicked");
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
                int squareSize = 200;
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
                    if(mouseX >= xy[0] && mouseX <= xy[0] + squareSize) {
                        if(mouseY >= xy[1] && mouseY <= xy[1] + squareSize) {
                            if(count < gameState.getMaps().size()) {
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
                DRAG_LOC[0] = (int) (mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize()) + Render.anchorUL[0];
                DRAG_LOC[1] = (int) (mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize()) + Render.anchorUL[1];
            }
        });
        // Staging and animator setup
        primaryStage.setScene(rootScene);
        primaryStage.show();
        AnimationTimer animator = new AnimationTimer() {
            @Override
            public void handle(long arg0) {
                currentTime = System.nanoTime();
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
     * @param args command line options - unused.
     */
    public static void main(String[] args) {
        String[] classPath = System.getProperty("java.class.path").split(";");
        if(!classPath[0].equals("")) {
            GAME_DATA_PATH = classPath[0] + File.separator + "GameData";
        } else {
            GAME_DATA_PATH = Paths.get("").toAbsolutePath().toString() + File.separator + "GameData";
        }
        GAME_ART_PATH = GAME_DATA_PATH + File.separator + "Art";
        GAME_MAP_PATH = GAME_DATA_PATH + File.separator + "Maps";
        programLogger.setLevel(Level.INFO);
        programLogger.log(Level.INFO, "GameData folder found at: " + GAME_DATA_PATH);
        programLogger.log(Level.INFO, "GameData/Art folder found at: " + GAME_ART_PATH);
        programLogger.log(Level.INFO, "GameData/Maps folder found at: " + GAME_MAP_PATH);
        String[] startUpSettings = FileOps.getFileLines(GAME_DATA_PATH + File.separator + "config.txt");
        for(String line: startUpSettings) {
            if(line.contains("SCREEN_WIDTH")) {
                SCREEN_WIDTH = Integer.parseInt(line.split("=")[1]);
            } else if(line.contains("SCREEN_HEIGHT")) {
                SCREEN_HEIGHT = Integer.parseInt(line.split("=")[1]);
            } else if(line.contains("SCREEN_MAP_HEIGHT")) {
                SCREEN_MAP_HEIGHT = Integer.parseInt(line.split("=")[1]);
            }
        }
        launch(args);
    }

}