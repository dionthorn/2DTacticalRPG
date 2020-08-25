package org.dionthorn;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Run will handle the core engine of the game including the JavaFX Application, rendering, updating, etc.
 */
public class Run extends Application {

    private final String PROGRAM_VERSION = "v0.2.1a";
    public static Logger programLogger = Logger.getLogger("programLogger");
    public static String GAME_DATA_PATH = "";
    private static final int SCREEN_WIDTH = 1024;
    private static final int SCREEN_HEIGHT = 1024;
    private static final int SCREEN_MAP_HEIGHT = 768;
    private static final int TILE_SIZE = 32;
    private int[] DRAG_LOC = {-1, -1};
    private int[] menuNewGameBounds;
    private int lastSelectedCharUID;
    private long FPS = TimeUnit.SECONDS.toNanos(1 / 30);
    private long startTime = System.nanoTime();
    private long currentTime;
    private Image mainMenuBg = new Image("file:" + GAME_DATA_PATH + "/Art/main_menu.png");
    private Image paperBg = new Image("file:" + GAME_DATA_PATH + "/Art/paper.png",
            SCREEN_WIDTH, SCREEN_HEIGHT-SCREEN_MAP_HEIGHT, false, false
    );
    private int battleFrameCounter = 0;
    private GraphicsContext gc;
    private GameState gameState;
    private DevMenu devMenu;

    /**
     * Called after every update()
     * will check gameState and decide what to render from that.
     */
    private void render() {
        if (gameState != null && devMenu != null) {
            Image selectedTileImg = gameState.getCurrentMap().getTile(devMenu.SELECTED_TILE_SET_ID,
                    devMenu.SELECTED_TILE_ID
            );
            ImageView selectedTileImgView = new ImageView(selectedTileImg);
            devMenu.getDevMenu().getChildren().add(selectedTileImgView);
            GridPane.setConstraints(selectedTileImgView, 4, 0);
            devMenu.getDevMenu().getChildren().remove(devMenu.getTileSetView());
            Image tileSet = gameState.getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTileSetSrc();
            devMenu.setTileSetView(new ImageView(tileSet));
            devMenu.getDevMenu().getChildren().add(devMenu.getTileSetView());
            GridPane.setConstraints(devMenu.getTileSetView(),
                    0, 2,
                    6, 6
            );
        }
        if (gameState == null || gameState.getCurrentState() == GameState.STATE.MAIN_MENU) {
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            gc.drawImage(mainMenuBg, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            gc.setTextAlign(TextAlignment.LEFT);
            Font menuTitleFont = new Font("Arial", 32);
            gc.setFont(menuTitleFont);
            gc.setFill(Color.WHITE);
            String title = "Game Project";
            Text menuTitle = new Text(title);
            menuTitle.setFont(menuTitleFont);
            gc.fillText(title, (SCREEN_WIDTH >> 1) - (menuTitle.getLayoutBounds().getWidth() / 2),
                    SCREEN_HEIGHT >> 4
            );
            Font menuOptionsFont = new Font("Arial", 28);
            gc.setFont(menuOptionsFont);
            String newGameString = "Play";
            Text newGameText = new Text(newGameString);
            newGameText.setFont(menuOptionsFont);
            menuNewGameBounds = new int[4];
            menuNewGameBounds[0] = (int) ((SCREEN_WIDTH >> 1) - (newGameText.getLayoutBounds().getWidth() / 2));
            menuNewGameBounds[1] = (SCREEN_HEIGHT >> 4) * 4;
            menuNewGameBounds[2] = (int) newGameText.getLayoutBounds().getWidth();
            menuNewGameBounds[3] = (int) newGameText.getLayoutBounds().getHeight();
            gc.fillText(newGameString, menuNewGameBounds[0], menuNewGameBounds[1]);
        } else if (gameState != null && gameState.getCurrentState() == GameState.STATE.GAME) {
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); // Clear canvas
            MapTile[][] mapTiles = gameState.getCurrentMap().getMapTiles();
            for (int y = 0; y < mapTiles.length; y++) {
                for (int x = 0; x < mapTiles[0].length; x++) {
                    gc.drawImage(gameState.getCurrentMap().getTile(mapTiles[y][x].getTileSet(),
                            mapTiles[y][x].getTileID()),
                            x * gameState.getCurrentMap().getTileSize(),
                            y * gameState.getCurrentMap().getTileSize()
                    );
                }
            }
            gc.drawImage(paperBg, 0, gameState.getCurrentMap().getMapHeight() * TILE_SIZE);
            for (Entity e : gameState.getEntities()) {
                if (e instanceof Drawable) {
                    if (((PhysicalEntity) e).getCurrentMap().equals(gameState.getCurrentMap())) {
                        if (e instanceof Character) {
                            if (!((Character) e).isAlive()) {
                                ((Character) e).setCurrentSprite(((Character) e).getCharClass().getDeadTileID());
                            }
                        }
                        ((Drawable) e).draw(gc);
                        if (e instanceof Character) {
                            double x, y, maxHP, pixelPerHP, currentHP, currentHPDisplayed, YaxisMod;
                            x = ((Character) e).getX();
                            y = ((Character) e).getY();
                            maxHP = ((Character) e).getMaxHP();
                            pixelPerHP = maxHP / TILE_SIZE;
                            currentHP = ((Character) e).getHp();
                            currentHPDisplayed = currentHP / pixelPerHP;
                            YaxisMod = (y * TILE_SIZE) + ((maxHP - currentHP) / pixelPerHP);
                            gc.setFill(Color.RED);
                            gc.setStroke(Color.BLACK);
                            if (gameState.getPlayerTeam().contains(e)) {
                                gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, 3, 32);
                                gc.setFill(Color.GREEN);
                                gc.setStroke(Color.BLACK);
                                if (currentHPDisplayed < 0) {
                                    currentHPDisplayed = 0;
                                }
                                gc.fillRect(x * TILE_SIZE, YaxisMod, 3, currentHPDisplayed);
                            } else if (gameState.getEnemyTeam().contains(e)) {
                                gc.fillRect((x * TILE_SIZE) + 29, y * TILE_SIZE, 3, 32);
                                gc.setFill(Color.GREEN);
                                gc.setStroke(Color.BLACK);
                                if (currentHPDisplayed < 0) {
                                    currentHPDisplayed = 0;
                                }
                                gc.fillRect((x * TILE_SIZE) + 29, YaxisMod, 3, currentHPDisplayed);
                            }
                        }
                    }
                }
            }
            if (!gameState.getNextTurn()) {
                String name = "";
                Entity target = null;
                for (Entity e : gameState.getEnemyTeam()) {
                    if (((Character) e).isMoveTurn()) {
                        name = ((Character) e).getName();
                        target = e;
                    }
                }
                for (Entity e : gameState.getPlayerTeam()) {
                    if (((Character) e).isMoveTurn()) {
                        name = ((Character) e).getName();
                        target = e;
                    }
                }
                if (!name.equals("")) {
                    if (name.equals(gameState.getPlayerEntity().getName())) {
                        gc.setStroke(Color.WHITE);
                        gc.setFill(Color.BLACK);
                        if (gameState.getPlayerEntity().isAlive()) {
                            gc.setTextAlign(TextAlignment.LEFT);
                            gc.fillText("Please Take Your Turn", 10, SCREEN_HEIGHT - 20);
                        } else {
                            gameState.setState(GameState.STATE.GAME_OVER);
                        }
                    } else {
                        if (((Character) target).isAlive()) {
                            gc.setStroke(Color.WHITE);
                            gc.setFill(Color.BLACK);
                            gc.setTextAlign(TextAlignment.LEFT);
                            gc.fillText(name + " Please Press Spacebar To Advance Their Turn",
                                    10, SCREEN_HEIGHT - 20);
                        } else {
                            if (!gameState.getNextTurn() && !gameState.getPlayerEntity().isMoveTurn()) {
                                gameState.setNextTurn(true);
                            }
                        }
                    }
                }
            }
        } else if (gameState != null && gameState.getCurrentState() == GameState.STATE.BATTLE) {
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            gc.setFill(Color.rgb(43, 107, 140));
            gc.fillRect(0, 0, SCREEN_WIDTH, SCREEN_MAP_HEIGHT);
            gc.drawImage(paperBg, 0, SCREEN_MAP_HEIGHT);
            gc.setFill(Color.GREY);
            int stageX, stageY, stageW, stageH;
            gc.fillRect(stageX = SCREEN_WIDTH >> 5, stageY = SCREEN_MAP_HEIGHT >> 5,
                    stageW = (SCREEN_WIDTH - (SCREEN_WIDTH >> 4)),
                    stageH = (SCREEN_MAP_HEIGHT >> 1) + (SCREEN_MAP_HEIGHT >> 3)
            );
            Character ally = gameState.getAttacker();
            Character enemy = gameState.getDefender();
            for (Entity c : gameState.getEnemyTeam()) {
                if (c == gameState.getAttacker()) {
                    ally = gameState.getDefender();
                    enemy = gameState.getAttacker();
                    break;
                }
            }
            int allyX, allyY, spriteSize = stageW >> 2;
            gc.drawImage(ally.currentSprite,
                    allyX = stageX + (stageW >> 3),
                    allyY = stageY + (stageH >> 2) + (stageH >> 4),
                    spriteSize, spriteSize
            );
            gc.setFill(Color.RED);
            gc.setStroke(Color.BLACK);
            double maxHP = ally.getMaxHP();
            double pixelPerHP = maxHP / (stageW >> 2);
            double currentHP = ally.getHp();
            double currentHPDisplayed = currentHP / pixelPerHP;
            int yAxisMod = stageY + (stageH >> 2) + (stageH >> 4) - 30;
            gc.fillRect(stageW / 6, yAxisMod, stageW >> 2, 10);
            gc.setFill(Color.GREEN);
            gc.fillRect(stageW / 6, yAxisMod, currentHPDisplayed, 10);
            gc.setFill(Color.BLACK);
            gc.setTextAlign(TextAlignment.LEFT);
            String heightTest = String.format("Name: %s", ally.getName());
            double textHeight = new Text(heightTest).getBoundsInLocal().getHeight() + 5;
            gc.fillText(heightTest, 50, SCREEN_MAP_HEIGHT + textHeight + 5);
            gc.fillText(String.format("HP: %.0f / %.0f", ally.getHp(), ally.getMaxHP()),
                    50, SCREEN_MAP_HEIGHT + textHeight * 3
            );
            int enemyX, enemyY;
            gc.drawImage(enemy.currentSprite,
                    enemyX = stageX + stageW - (stageW >> 3) - (stageW >> 2),
                    enemyY = stageY + (stageH >> 2) + (stageH >> 4),
                    stageW >> 2, stageW >> 2
            );
            gc.setFill(Color.RED);
            gc.setStroke(Color.BLACK);
            maxHP = enemy.getMaxHP();
            pixelPerHP = maxHP / (stageW >> 2);
            currentHP = enemy.getHp();
            currentHPDisplayed = currentHP / pixelPerHP;
            gc.fillRect(enemyX, yAxisMod, stageW >> 2, 10);
            gc.setFill(Color.GREEN);
            gc.fillRect(enemyX, yAxisMod, currentHPDisplayed, 10);
            gc.setFill(Color.BLACK);
            gc.fillText(String.format("Name: %s", enemy.getName()), (SCREEN_WIDTH >> 1) + 50,
                    SCREEN_MAP_HEIGHT + textHeight + 5
            );
            gc.fillText(String.format("HP: %.0f / %.0f", enemy.getHp(), enemy.getMaxHP()),
                    (SCREEN_WIDTH >> 1) + 50, SCREEN_MAP_HEIGHT + textHeight * 3
            );
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(5);
            gc.strokeRect(SCREEN_WIDTH >> 5, SCREEN_MAP_HEIGHT >> 5,
                    (SCREEN_WIDTH - (SCREEN_WIDTH >> 4)), (SCREEN_MAP_HEIGHT >> 1) + (SCREEN_MAP_HEIGHT >> 3)
            );
            gc.setFill(Color.GREY);
            gc.setLineWidth(2);
            final int y1 = (SCREEN_MAP_HEIGHT >> 1) + (SCREEN_MAP_HEIGHT >> 3) + (SCREEN_MAP_HEIGHT >> 4);
            gc.fillRect(SCREEN_WIDTH >> 4, y1,
                    (SCREEN_WIDTH >> 2) + (SCREEN_WIDTH >> 3), SCREEN_MAP_HEIGHT >> 2
            );
            gc.strokeRect(SCREEN_WIDTH >> 4, y1,
                    (SCREEN_WIDTH >> 2) + (SCREEN_WIDTH >> 3), SCREEN_MAP_HEIGHT >> 2
            );
            gc.setTextAlign(TextAlignment.CENTER);
            final int y2 = (SCREEN_MAP_HEIGHT >> 1) + (SCREEN_MAP_HEIGHT >> 2) + (SCREEN_MAP_HEIGHT >> 4);
            gc.strokeText("ATTACK", SCREEN_WIDTH >> 2, y2);
            gc.fillRect((SCREEN_WIDTH >> 1) + (SCREEN_WIDTH >> 4), y1,
                    (SCREEN_WIDTH >> 2) + (SCREEN_WIDTH >> 3), SCREEN_MAP_HEIGHT >> 2
            );
            gc.strokeRect((SCREEN_WIDTH >> 1) + (SCREEN_WIDTH >> 4), y1,
                    (SCREEN_WIDTH >> 2) + (SCREEN_WIDTH >> 3), SCREEN_MAP_HEIGHT >> 2
            );
            gc.strokeText("DEFEND", SCREEN_WIDTH - (SCREEN_WIDTH >> 2), y2);
            battleFrameCounter++;
            if (ally.isBattleTurn()) {
                if (gameState.getPlayerEntity().equals(ally)) {
                    if (ally.IsAttacking()) {
                        if (ally.getCharClass().getCompletedCycles() > 2) {
                            ally.setIsAttacking(false);
                            ally.setBattleTurn(false);
                            enemy.setBattleTurn(true);
                            ally.attack(enemy);
                            ally.getCharClass().setCompletedCycles(0);
                            if (!enemy.isAlive()) {
                                boolean didLevel = ally.getCharClass().addXP(
                                        1000 * enemy.getCharClass().getLevel()
                                );
                                if (didLevel) {
                                    ally.levelUp();
                                }
                                gameState.setState(GameState.STATE.GAME);
                            }
                        }
                        ally.attackAnimation(battleFrameCounter);
                    }
                } else {
                    if (ally.IsAttacking()) {
                        if (ally.getCharClass().getCompletedCycles() > 2) {
                            ally.setIsAttacking(false);
                            ally.setBattleTurn(false);
                            enemy.setBattleTurn(true);
                            ally.attack(enemy);
                            ally.getCharClass().setCompletedCycles(0);
                            if (!enemy.isAlive()) {
                                boolean didLevel = ally.getCharClass().addXP(
                                        1000 * enemy.getCharClass().getLevel()
                                );
                                if (didLevel) {
                                    ally.levelUp();
                                }
                                gameState.setState(GameState.STATE.GAME);
                            }
                        }
                        ally.attackAnimation(battleFrameCounter);
                    } else {
                        ally.setIsAttacking(true);
                    }
                }
                gc.drawImage(ally.getCurrentSprite(), allyX, allyY, spriteSize, spriteSize);
                gc.drawImage(enemy.getCurrentSprite(), enemyX, enemyY, spriteSize, spriteSize);
            } else if (enemy.isBattleTurn()) {
                if (enemy.IsAttacking()) {
                    if (enemy.getCharClass().getCompletedCycles() > 2) {
                        enemy.setIsAttacking(false);
                        enemy.setBattleTurn(false);
                        ally.setBattleTurn(true);
                        enemy.attack(ally);
                        enemy.getCharClass().setCompletedCycles(0);
                        if (!ally.isAlive()) {
                            if (ally.equals(gameState.getPlayerEntity())) {
                                gameState.setState(GameState.STATE.GAME_OVER);
                            } else {
                                boolean didLevel = enemy.getCharClass().addXP(
                                        1000 * ally.getCharClass().getLevel()
                                );
                                if (didLevel) {
                                    enemy.levelUp();
                                }
                                gameState.setState(GameState.STATE.GAME);
                            }
                        }
                    }
                    enemy.attackAnimation(battleFrameCounter);
                } else {
                    enemy.setIsAttacking(true);
                }
                gc.drawImage(ally.getCurrentSprite(), allyX, allyY, spriteSize, spriteSize);
                gc.drawImage(enemy.getCurrentSprite(), enemyX, enemyY, spriteSize, spriteSize);
            }
            if (battleFrameCounter == 30) {
                battleFrameCounter = 0;
            }
        } else if(gameState != null && gameState.getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            gc.drawImage(mainMenuBg, 0, 0, SCREEN_WIDTH, SCREEN_MAP_HEIGHT);
            gc.setFill(Color.rgb(43, 107, 140));
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
            for(int[] xy: squareXY) {
                gc.fillRect(xy[0], xy[1], squareSize, squareSize);
            }
            int count = 0;
            gc.setFill(Color.BLACK);
            gc.setFont(new Font(12));
            for(Map m: gameState.getMaps()) {
                String[] path = m.getPATH().split("/");
                int index;
                index = Math.max((path.length - 1), 0);
                String iconName = path[index].split("\\\\")[1].split("\\.")[0] + "_Icon.png";
                if(FileOps.doesFileExist(GAME_DATA_PATH + "/Art/" + iconName)) {
                    gameState.getMaps().get(count).setIcon(new Image("file:" + GAME_DATA_PATH + "/Art/" + iconName));
                    gc.drawImage(gameState.getMaps().get(count).getIcon(), squareXY[count][0], squareXY[count][1]);
                } else {
                    gc.fillText(path[index], squareXY[count][0]+10, squareXY[count][1]+10);
                }
                count++;
            }
            gc.drawImage(paperBg, 0, SCREEN_MAP_HEIGHT);
            gc.setFont(new Font("Arial", 32));
            String[] path = gameState.getCurrentMap().getPATH().split("/");
            gc.fillText(path[path.length - 1], SCREEN_WIDTH>>4, SCREEN_MAP_HEIGHT + (SCREEN_MAP_HEIGHT>>4));
            gc.fillText("Press Enter to Load The Selected Map", SCREEN_WIDTH>>4,
                    SCREEN_MAP_HEIGHT + (SCREEN_MAP_HEIGHT>>2)
            );
        } else if(gameState != null && gameState.getCurrentState() == GameState.STATE.GAME_OVER) {
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            gc.drawImage(mainMenuBg, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            Text gameOver = new Text("GAME OVER!");
            gc.fillText(gameOver.getText(), SCREEN_WIDTH >> 1, 40);
            gc.fillText("Press ESC to exit to Main Menu!", SCREEN_WIDTH >> 1, 100);
        }
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
        for(String path: FileOps.getFileNamesFromDirectory(GAME_DATA_PATH + "/Maps/")) {
            if(!path.equals("config.dat") && !path.equals(".gitattributes") && !path.contains("meta")) {
                if(gameState != null) {
                    gameState.getMaps().add(new Map(GAME_DATA_PATH + "/Maps" + File.separator + path));
                } else {
                    gameState = new GameState(new Map(GAME_DATA_PATH + "/Maps" + File.separator + path));
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
                    if(devMenu == null) {
                        devMenu = new DevMenu(this);
                        devMenu.getDevMapPath().setText(String.format("%s", gameState.getCurrentMap().getPATH()));
                        for(Map toLoad: gameState.getMaps()) {
                            devMenu.getMapList().getItems().add(toLoad.getPATH());
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
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            int mouseX = (int) mouseEvent.getX();
            int mouseY = (int) mouseEvent.getY();
            programLogger.log(Level.INFO, "Mouse Event: (" + mouseX + ", " + mouseY + ")");
            if(gameState != null && devMenu != null && devMenu.EDIT_MODE &&
                    gameState.getCurrentState() == GameState.STATE.GAME) {
                int tileX = mouseX / gameState.getCurrentMap().getTileSize();
                int tileY = mouseY / gameState.getCurrentMap().getTileSize();
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
                        int releasedX = (int) mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize();
                        int releasedY = (int) mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize();
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
                        int releasedX = (int) mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize();
                        int releasedY = (int) mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize();
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
                int xLoc = menuNewGameBounds[0];
                int yLoc = menuNewGameBounds[1];
                int xWidth = menuNewGameBounds[2];
                int yHeight = menuNewGameBounds[3];
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
        canvas.addEventHandler(MouseDragEvent.DRAG_DETECTED, (mouseEvent) -> {
            if(gameState != null && devMenu.EDIT_MODE && gameState.getCurrentState() == GameState.STATE.GAME) {
                DRAG_LOC[0] = (int) mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize();
                DRAG_LOC[1] = (int) mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize();
            }
        });
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

    /**
     * Return the int array [width, height] in tiles of the current map area
     * @return the int array [width, height] in tiles of the current map area
     */
    public static int[] getMapDimensions() {
        int[] result = new int[2];
        result[0] = SCREEN_WIDTH/TILE_SIZE;
        result[1] = SCREEN_MAP_HEIGHT/TILE_SIZE;
        return result;
    }

    /**
     * Returns the program version string.
     * @return the program version string
     */
    public String getProgramVersion() { return PROGRAM_VERSION; }

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

    public Canvas getCanvas() { return gc.getCanvas(); }

    /**
     * The entry point for the program. We determine where /GameData/ folder is here.
     * @param args command line options - unused.
     */
    public static void main(String[] args) {
        String[] classPath = System.getProperty("java.class.path").split(";");
        if(!classPath[0].equals("")) {
            GAME_DATA_PATH = classPath[0] + "/GameData";
        } else {
            GAME_DATA_PATH = Paths.get("").toAbsolutePath().toString() + "/GameData";
        }
        programLogger.setLevel(Level.INFO);
        programLogger.log(Level.INFO, "GameData folder found at: " + GAME_DATA_PATH);
        launch(args);
    }

}