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

public class Run extends Application {
    /*
        This java class is responsible for all GUI handling including keyboard/mouse inputs
        This java class will hold a reference to a single GameState object which is used to control game flow
     */
    private final String PROGRAM_VERSION = "v0.2.0a"; // Constant to identify Program Version
    public static String GAME_DATA_PATH = "";
    public static boolean DEBUG_OUTPUT = false; // If true will allow debug information to be printed to console
    // Wrap System.out.println call with if(DEBUG_OUTPUT) {} to allow it to work or in other classes Run.DEBUG_OUTPUT
    private static final int SCREEN_WIDTH = 1024; // Constant of screen width in pixels
    private static final int SCREEN_HEIGHT = 1024; // Constant of screen height in pixels
    private static final int SCREEN_MAP_HEIGHT = 768; // to show end of map drawing area
    private static final int TILE_SIZE = 32; // Hardcoded 32*32 pixels8
    private int[] DRAG_LOC = {-1, -1}; // This is a sentinel value that when -1 means there is no mouse drag to process
    // if it is set to a positive value it will be the mouse(x,y) upon the start of a drag
    private int[] menuNewGameBounds; // Used to determine where new game button is on the main menu
    private int lastSelectedCharUID;
    private long FPS = TimeUnit.SECONDS.toNanos(1 / 30); // 30 FPS used by AnimationTimer
    private long startTime = System.nanoTime(); // These two are switches used for render/update logic
    private long currentTime;
    private Image mainMenuBg = new Image("file:" + GAME_DATA_PATH + "/Art/main_menu.png");
    private Image paperBg = new Image("file:" + GAME_DATA_PATH + "/Art/paper.png", SCREEN_WIDTH, SCREEN_HEIGHT-SCREEN_MAP_HEIGHT, false, false);
    private int battleFrameCounter = 0; // Used to count frames during battle animations,
    // a value of 30 means ~1 sec has passed and should be set back to 0.
    private GraphicsContext gc; // Directly access draw methods on the canvas
    private GameState gameState; // Now controls the maps/entities and general game flow via flags
    private DevMenu devMenu;

    private void render() {
        // Render the frame based on GameState.currentState
        if (gameState != null && devMenu != null) {
            // Load dev menu related rendering
            Image selectedTileImg = gameState.getCurrentMap().getTile(devMenu.SELECTED_TILE_SET_ID,
                    devMenu.SELECTED_TILE_ID);
            ImageView selectedTileImgView = new ImageView(selectedTileImg);
            devMenu.getDevMenu().getChildren().add(selectedTileImgView);
            GridPane.setConstraints(selectedTileImgView, 4, 0);

            devMenu.getDevMenu().getChildren().remove(devMenu.getTileSetView());
            Image tileSet = gameState.getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTileSetSrc();
            devMenu.setTileSetView(new ImageView(tileSet));
            devMenu.getDevMenu().getChildren().add(devMenu.getTileSetView());
            GridPane.setConstraints(devMenu.getTileSetView(), 0, 2,
                    6, 6);
        }
        if (gameState == null || gameState.getCurrentState() == GameState.STATE.MAIN_MENU) {
            // Main menu rendering
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); // Clear canvas
            // Perhaps load a menuBackground.png here
            gc.drawImage(mainMenuBg, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            // Draw the menu title as dynamically as possible
            gc.setTextAlign(TextAlignment.LEFT);
            Font menuTitleFont = new Font("Arial", 32); // Can change to whatever font we wish
            gc.setFont(menuTitleFont);
            gc.setFill(Color.WHITE);  // Can change text color as well
            String title = "Game Project";
            Text menuTitle = new Text(title);
            menuTitle.setFont(menuTitleFont);
            gc.fillText(title, (SCREEN_WIDTH >> 1) - (menuTitle.getLayoutBounds().getWidth() / 2),
                    SCREEN_HEIGHT >> 4);
            Font menuOptionsFont = new Font("Arial", 28); // Can change to whatever font we wish
            gc.setFont(menuOptionsFont);
            String newGameString = "Play";
            Text newGameText = new Text(newGameString);
            newGameText.setFont(menuOptionsFont);
            menuNewGameBounds = new int[4]; // x, y, width, height used to identify a rectangle
            // Around the bounds of the "Play" text
            menuNewGameBounds[0] = (int) ((SCREEN_WIDTH >> 1) - (newGameText.getLayoutBounds().getWidth() / 2));
            menuNewGameBounds[1] = (SCREEN_HEIGHT >> 4) * 4;
            menuNewGameBounds[2] = (int) newGameText.getLayoutBounds().getWidth();
            menuNewGameBounds[3] = (int) newGameText.getLayoutBounds().getHeight();
            gc.fillText(newGameString, menuNewGameBounds[0], menuNewGameBounds[1]);
        } else if (gameState != null && gameState.getCurrentState() == GameState.STATE.GAME) {
            // Graphics logic for displaying a Map to the screen one tile at a time and all present characters
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); // Clear canvas
            MapTile[][] mapTiles = gameState.getCurrentMap().getMapTiles();

            for (int y = 0; y < mapTiles.length; y++) {
                for (int x = 0; x < mapTiles[0].length; x++) {
                    gc.drawImage(gameState.getCurrentMap().getTile(mapTiles[y][x].getTileSet(), mapTiles[y][x].getTileID()),
                            x * gameState.getCurrentMap().getTileSize(),
                            y * gameState.getCurrentMap().getTileSize()
                    );
                }
            }
            gc.drawImage(paperBg, 0, gameState.getCurrentMap().getMapHeight() * TILE_SIZE);
            // Draw all entities that have drawable and are in the currentMap
            for (Entity e : gameState.getEntities()) {
                if (e instanceof Drawable) {
                    if (((PhysicalEntity) e).getCurrentMap().equals(gameState.getCurrentMap())) {
                        // Only draw entities tied to the current map
                        if (e instanceof Character) {
                            if (!((Character) e).isAlive()) {
                                ((Character) e).setCurrentSprite(((Character) e).getCharClass().getDeadTileID());
                            }
                        }
                        ((Drawable) e).draw(gc); // This draws the sprite
                        if (e instanceof Character) {
                            double x, y, maxHP, pixelPerHP, currentHP, currentHPDisplayed, YaxisMod;
                            x = ((Character) e).getX();
                            y = ((Character) e).getY();
                            maxHP = ((Character) e).getMaxHP();
                            // 32 pixels is max bar size, one tile that character occupies
                            // Max hp for level 1 = 50. 50hp / 32 total pixels = 1.5625 hp per pixel
                            pixelPerHP = maxHP / TILE_SIZE;
                            currentHP = ((Character) e).getHp();
                            // If current HP is 25hp and hp per pixel is 1.5625 then 25/1.5625 = 16 pixels width
                            currentHPDisplayed = currentHP / pixelPerHP;
                            YaxisMod = (y * TILE_SIZE) + ((maxHP - currentHP) / pixelPerHP); // makes the green bar go down

                            // Draw max hp red rectangle
                            gc.setFill(Color.RED);
                            gc.setStroke(Color.BLACK);
                            if (gameState.getPlayerTeam().contains(e)) {
                                gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, 3, 32);
                                // Draw current hp green rectangle on top of it
                                gc.setFill(Color.GREEN);
                                gc.setStroke(Color.BLACK);
                                if (currentHPDisplayed < 0) {
                                    currentHPDisplayed = 0;
                                }
                                gc.fillRect(x * TILE_SIZE, YaxisMod, 3, currentHPDisplayed);
                            } else if (gameState.getEnemyTeam().contains(e)) {
                                gc.fillRect((x * TILE_SIZE) + 29, y * TILE_SIZE, 3, 32);
                                // Draw current hp green rectangle on top of it
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
                // Turn indicator
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
                        if (target != null && ((Character) target).isAlive()) {
                            gc.setStroke(Color.WHITE);
                            gc.setFill(Color.BLACK);
                            gc.setTextAlign(TextAlignment.LEFT);
                            gc.fillText(name + " Please Press Spacebar To Advance Their Turn", 10, SCREEN_HEIGHT - 20);
                        } else {
                            if (!gameState.getNextTurn() && !gameState.getPlayerEntity().isMoveTurn()) {
                                gameState.setNextTurn(true);
                            }
                        }
                    }
                }
            }
        } else if (gameState != null && gameState.getCurrentState() == GameState.STATE.BATTLE) {
            // Battle Scene
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            gc.setFill(Color.rgb(43, 107, 140)); //To be changed to the more appealing 43,107,140 once
            //I figure out why custom color makes it freak out.
            //For Beta: Creating an image for the border might look nice
            gc.fillRect(0, 0, SCREEN_WIDTH, SCREEN_MAP_HEIGHT); //set HUD background

            // Put character stats here
            gc.drawImage(paperBg, 0, SCREEN_MAP_HEIGHT);

            gc.setFill(Color.GREY);

            //draw the battle stage as dynamically as possible
            //Battle stage should take up 15/16 width, and 5/8 height,
            //centered with even border on left and right, 1/32 height border on top
            int stageX, stageY, stageW, stageH;//Variables to store the location and size of the battle stage


            gc.fillRect(stageX = SCREEN_WIDTH >> 5, stageY = SCREEN_MAP_HEIGHT >> 5, //this will eventually be replaced
                    stageW = (SCREEN_WIDTH - (SCREEN_WIDTH >> 4)), stageH = (SCREEN_MAP_HEIGHT >> 1) + (SCREEN_MAP_HEIGHT >> 3)); //with the background image of the fight scene


            //Draw the fighters onto the stage

            Character ally = gameState.getAttacker();
            Character enemy = gameState.getDefender();

            for (Entity c : gameState.getEnemyTeam()) {
                if (c == gameState.getAttacker()) {
                    ally = gameState.getDefender();
                    enemy = gameState.getAttacker();  //Make sure each person is on the correct side of the screen
                    //regardless of who is attacking
                }
            }

            // Draw Ally
            int allyX, allyY, spriteSize = stageW >> 2;
            gc.drawImage(ally.currentSprite,
                    allyX = stageX + (stageW >> 3), allyY = stageY + (stageH >> 2) + (stageH >> 4),
                    spriteSize, spriteSize);//keep sprite square
            gc.setFill(Color.RED); // max hp is red, current hp is green
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
            gc.fillText(String.format("HP: %.0f / %.0f", ally.getHp(), ally.getMaxHP()), 50, SCREEN_MAP_HEIGHT + textHeight * 3);

            // Draw Enemy
            int enemyX, enemyY;
            gc.drawImage(enemy.currentSprite,
                    enemyX = stageX + stageW - (stageW >> 3) - (stageW >> 2), enemyY = stageY + (stageH >> 2) + (stageH >> 4),
                    stageW >> 2, stageW >> 2);//keep sprite square
            gc.setFill(Color.RED); // max hp is red, current hp is green
            gc.setStroke(Color.BLACK);
            maxHP = enemy.getMaxHP();
            pixelPerHP = maxHP / (stageW >> 2);
            currentHP = enemy.getHp();
            currentHPDisplayed = currentHP / pixelPerHP;
            gc.fillRect(enemyX, yAxisMod, stageW >> 2, 10);
            gc.setFill(Color.GREEN);
            gc.fillRect(enemyX, yAxisMod, currentHPDisplayed, 10);
            gc.setFill(Color.BLACK);
            gc.fillText(String.format("Name: %s", enemy.getName()), (SCREEN_WIDTH >> 1) + 50, SCREEN_MAP_HEIGHT + textHeight + 5);
            gc.fillText(String.format("HP: %.0f / %.0f", enemy.getHp(), enemy.getMaxHP()), (SCREEN_WIDTH >> 1) + 50, SCREEN_MAP_HEIGHT + textHeight * 3);

            //draw Border for fight scene
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(5);
            gc.strokeRect(SCREEN_WIDTH >> 5, SCREEN_MAP_HEIGHT >> 5, (SCREEN_WIDTH - (SCREEN_WIDTH >> 4)), (SCREEN_MAP_HEIGHT >> 1) + (SCREEN_MAP_HEIGHT >> 3));

            //Create Buttons
            gc.setFill(Color.GREY);
            gc.setLineWidth(2);

            //ATTACK BUTTON,
            // 1/16 width from left, 1/16 height from bottom of the stage (1/2+1/8+1/16 from top)
            // 3/8 width, 1/4 height
            final int y1 = (SCREEN_MAP_HEIGHT >> 1) + (SCREEN_MAP_HEIGHT >> 3) + (SCREEN_MAP_HEIGHT >> 4);
            gc.fillRect(SCREEN_WIDTH >> 4, y1,
                    (SCREEN_WIDTH >> 2) + (SCREEN_WIDTH >> 3), SCREEN_MAP_HEIGHT >> 2);
            gc.strokeRect(SCREEN_WIDTH >> 4, y1,
                    (SCREEN_WIDTH >> 2) + (SCREEN_WIDTH >> 3), SCREEN_MAP_HEIGHT >> 2);
            gc.setTextAlign(TextAlignment.CENTER);
            //Next Line will be replaced with image in beta
            final int y2 = (SCREEN_MAP_HEIGHT >> 1) + (SCREEN_MAP_HEIGHT >> 2) + (SCREEN_MAP_HEIGHT >> 4);
            gc.strokeText("ATTACK", SCREEN_WIDTH >> 2, y2);

            //DEFEND BUTTON
            // 9/16 (1/2+1/16), 1/16 height from bottom of the stage
            // 3/8 width, 1/4 height
            gc.fillRect((SCREEN_WIDTH >> 1) + (SCREEN_WIDTH >> 4), y1,
                    (SCREEN_WIDTH >> 2) + (SCREEN_WIDTH >> 3), SCREEN_MAP_HEIGHT >> 2);
            gc.strokeRect((SCREEN_WIDTH >> 1) + (SCREEN_WIDTH >> 4), y1,
                    (SCREEN_WIDTH >> 2) + (SCREEN_WIDTH >> 3), SCREEN_MAP_HEIGHT >> 2);
            //Next Line will be replaced with image in beta
            gc.strokeText("DEFEND", SCREEN_WIDTH - (SCREEN_WIDTH >> 2), y2);

            battleFrameCounter++;

            // animation logic
            if (ally.isBattleTurn()) {
                if (gameState.getPlayerEntity().equals(ally)) {
                    // Wait for Player Input on the attack button
                    if (ally.IsAttacking()) {
                        if (ally.getCharClass().getCompletedCycles() > 2) {
                            ally.setIsAttacking(false);
                            ally.setBattleTurn(false);
                            enemy.setBattleTurn(true);
                            ally.attack(enemy);
                            ally.getCharClass().setCompletedCycles(0);
                            if (!enemy.isAlive()) {
                                boolean didLevel = ally.getCharClass().addXP(1000 * enemy.getCharClass().getLevel()); // changed to just 1000 xp gain per kill per level
                                if (didLevel) {
                                    ally.levelUp();
                                }
                                gameState.setState(GameState.STATE.GAME);
                            }
                        }
                        ally.attackAnimation(battleFrameCounter);
                    }
                    gc.drawImage(ally.getCurrentSprite(), allyX, allyY, spriteSize, spriteSize);
                    gc.drawImage(enemy.getCurrentSprite(), enemyX, enemyY, spriteSize, spriteSize);
                } else {
                    if (ally.IsAttacking()) {
                        if (ally.getCharClass().getCompletedCycles() > 2) {
                            ally.setIsAttacking(false);
                            ally.setBattleTurn(false);
                            enemy.setBattleTurn(true);
                            ally.attack(enemy);
                            ally.getCharClass().setCompletedCycles(0);
                            if (!enemy.isAlive()) {
                                boolean didLevel = ally.getCharClass().addXP(1000 * enemy.getCharClass().getLevel()); // changed to just 1000 xp gain per kill per level
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
                    gc.drawImage(ally.getCurrentSprite(), allyX, allyY, spriteSize, spriteSize);
                    gc.drawImage(enemy.getCurrentSprite(), enemyX, enemyY, spriteSize, spriteSize);
                }
            } else if (enemy.isBattleTurn()) {
                // enemy is always AI so no need to check for player
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
                                boolean didLevel = enemy.getCharClass().addXP(1000 * ally.getCharClass().getLevel()); // changed to just 1000 xp gain per kill per level
                                if (didLevel) {
                                    enemy.levelUp();
                                }
                                gameState.setState(GameState.STATE.GAME);
                            }
                        }
                    }
                    enemy.attackAnimation(battleFrameCounter);
                } else {
                    // for now enemy always chooses attack
                    enemy.setIsAttacking(true);
                }
                gc.drawImage(ally.getCurrentSprite(), allyX, allyY, spriteSize, spriteSize);
                gc.drawImage(enemy.getCurrentSprite(), enemyX, enemyY, spriteSize, spriteSize);
            }
            // reset battleFrameCounter every 30 frames
            if (battleFrameCounter == 30) {
                battleFrameCounter = 0;
            }
        } else if(gameState != null && gameState.getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
            int selectedBox = 0;
            // draw boxes for maps in memory, maybe 4 boxes and use arrow keys to select different maps
            gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            gc.drawImage(mainMenuBg, 0, 0, SCREEN_WIDTH, SCREEN_MAP_HEIGHT);
            gc.setFill(Color.rgb(43, 107, 140));

            // Draw map selection squares
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

            // Draw map names into squares
            int count = 0;
            gc.setFill(Color.BLACK);
            gc.setFont(new Font(12));
            for(Map m: gameState.getMaps()) {
                String[] path = m.getPATH().split("/");
                int index = 0;
                if((path.length - 1) < 0) {
                    index = 0;
                } else {
                    index = path.length - 1;
                }
                gc.fillText(path[index], squareXY[count][0]+10, squareXY[count][1]+10);
                count++;
            }

            // draw lower paper 'console' area
            gc.drawImage(paperBg, 0, SCREEN_MAP_HEIGHT);
            gc.setFont(new Font("Arial", 32));
            String[] path = gameState.getCurrentMap().getPATH().split("/");
            gc.fillText(path[path.length - 1], SCREEN_WIDTH>>4, SCREEN_MAP_HEIGHT + (SCREEN_MAP_HEIGHT>>4));

            gc.fillText("Press Enter to Load The Selected Map", SCREEN_WIDTH>>4, SCREEN_MAP_HEIGHT + (SCREEN_MAP_HEIGHT>>2));

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

    public void update() {
        // Update game logic in between frames only update if a gameState exists and in GAME state
        if(gameState != null && gameState.getCurrentState() == GameState.STATE.GAME) {
            for(Entity e: gameState.getEntities()) { // Go through all entities
                if(e instanceof Updateable) { // Only try to check ones that are updatable and on current map
                    if( ((PhysicalEntity) e).getCurrentMap().equals(gameState.getCurrentMap()) ) {
                        ((Updateable) e).update(gameState);
                    }
                }
            }
        }
    }

    private void newGame() {
        // When they click Play on the Main Menu this starts the initialization of a new game
        // First we will load the first map in the GameData directory
        for(String path: getFileNamesFromDirectory(GAME_DATA_PATH + "/Maps/")) {
            // Be sure to ignore any non map files
            if(!path.equals("config.dat") && !path.equals(".gitattributes") && !path.contains("meta")) {
                // properly start an entirely new game.
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
        // Create the new Player. In the future the CHARACTER_CREATION gameState
        // will be the first to load and this will all edited there for now just generate a default Player UID=0.
        gameState.getEntities().add(
                new Player(
                        gameState.getCurrentMap(),
                        "MartialClassPlayer.png", "Player",
                        Integer.parseInt(startLoc[0]), Integer.parseInt(startLoc[1]), // x, y pull from meta.
                        new MartialClass()
                )
        );
        gameState.getPlayerTeam().add(gameState.getPlayerEntity());
        // Generate default maps allies/enemies
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
                NonPlayerCharacter tempChar = new NonPlayerCharacter(gameState.getCurrentMap(), tempClass.getDefaultSpriteAlly(), name, x, y, tempClass);
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
                NonPlayerCharacter tempChar = new NonPlayerCharacter(gameState.getCurrentMap(), tempClass.getDefaultSpriteEnemy(), name, x, y, tempClass);
                gameState.getEntities().add(tempChar);
                gameState.getEnemyTeam().add(tempChar);
            }
        }
        // Move to Level Selection
        gameState.setState(GameState.STATE.LEVEL_SELECTION);
    }

    // GUI logic for JavaFX
    @Override
    public void start(Stage primaryStage) {
        // Set properties of the Stage object
        primaryStage.setTitle("OOP Game Project " + PROGRAM_VERSION); // Set the title of the window
        primaryStage.setResizable(false); // Do not allow resize of screen width/height
        // Build a Group/Scene with a black background and create a new Canvas Object
        // The Canvas is the actual game screen area of the Window
        // The Graphics Context is used to draw onto the canvas and is needed to draw objects
        Group rootGroup = new Group();
        Scene rootScene = new Scene(rootGroup, SCREEN_WIDTH, SCREEN_HEIGHT, Color.BLACK);
        primaryStage.sizeToScene();
        // The main node we draw onto think of it like a painting canvas.
        Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        rootGroup.getChildren().add(canvas);
        // Keyboard Input Handling for the Game Window is processed here.
        rootScene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if (DEBUG_OUTPUT) {
                System.out.println("[DEBUG] KeyInput: " + key.getCode());
            }
            // Example of how to use key handling to make the escape key close the game (will make exit to menu later)
            if (gameState == null || gameState.getCurrentState() == GameState.STATE.MAIN_MENU) {
                if (key.getCode() == KeyCode.ESCAPE) {
                    // If in main menu and you press escape it will close the window and exit the JVM
                    primaryStage.close();
                    System.exit(0);
                }
            } else if(gameState.getCurrentState() == GameState.STATE.GAME) {
                if(key.getCode() == KeyCode.ESCAPE) {
                    // Exit to Main Menu
                    gameState = null;
                }
                if(key.getCode() == KeyCode.BACK_QUOTE) {
                    // DEV MENU
                    if(devMenu == null) {
                        devMenu = new DevMenu(this); // create the devMenuWindow and pass this app as a reference
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
                    // Move Up
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
                    // Move Right
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
                    // Move Left
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
                    // Move Down
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
                    // Advance the turn
                    if(!gameState.getNextTurn() && !gameState.getPlayerEntity().isMoveTurn()) {
                        gameState.setNextTurn(true);
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.BATTLE) {
                if (key.getCode() == KeyCode.ESCAPE) {
                    // For now just exit back to map view
                    gameState.setState(GameState.STATE.GAME);
                } else if (key.getCode() == KeyCode.A) {
                    if (DEBUG_OUTPUT) {
                        System.out.println("Name: " + gameState.getAttacker().getName());
                        System.out.println("IsAttacking: " + gameState.getAttacker().IsAttacking());
                        System.out.println("isBattleTurn: " + gameState.getAttacker().isBattleTurn());
                        System.out.println("CompletedCycles: " + gameState.getAttacker().getCharClass().getCompletedCycles());
                    }
                } else if (key.getCode() == KeyCode.D) {
                    if (DEBUG_OUTPUT) {
                        System.out.println("Name: " + gameState.getDefender().getName());
                        System.out.println("IsAttacking: " + gameState.getDefender().IsAttacking());
                        System.out.println("isBattleTurn: " + gameState.getDefender().isBattleTurn());
                        System.out.println("CompletedCycles: " + gameState.getDefender().getCharClass().getCompletedCycles());
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
                if(key.getCode() == KeyCode.ENTER) {
                    // if you press enter on the level selection screen the game will load the selected map
                    gameState.setState(GameState.STATE.GAME);
                    gameState.getPlayerEntity().setMoveTurn(true);
                }
            } else if(gameState.getCurrentState() == GameState.STATE.GAME_OVER) {
                if(key.getCode() == KeyCode.ESCAPE) {
                    // If you press escape on game over screen it'll exit to the main menu.
                    gameState = null;
                }
            }
        });

        // Mouse Input handling if needed this is the shell
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            int mouseX = (int) mouseEvent.getX(); // This pulls the x and y coordinate from the mouseEvent
            int mouseY = (int) mouseEvent.getY();
            if(DEBUG_OUTPUT) {
                System.out.println("Mouse Event: (" + mouseX + ", " + mouseY + ")");
            }
            if(gameState != null && devMenu != null && devMenu.EDIT_MODE && gameState.getCurrentState() == GameState.STATE.GAME) {
                int tileX = mouseX / gameState.getCurrentMap().getTileSize();
                int tileY = mouseY / gameState.getCurrentMap().getTileSize();
                //tileX += gameState.getCurrentMap().getXOffset();
                //tileY += gameState.getCurrentMap().getYOffset();
                if(DEBUG_OUTPUT) {
                    System.out.println("x,y: " + tileX + " " + tileY);
                }
                if (DRAG_LOC[0] == -1) {
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        MapTile[][] tempMapTiles = gameState.getCurrentMap().getMapTiles();
                        if(DEBUG_OUTPUT) {
                            System.out.println("TILEID: " + tempMapTiles[tileY][tileX].getTileID());
                        }
                        tempMapTiles[tileY][tileX].setTileID(devMenu.SELECTED_TILE_ID);
                        tempMapTiles[tileY][tileX].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                        gameState.getCurrentMap().setMapTiles(tempMapTiles);
                        if(DEBUG_OUTPUT) {
                            System.out.println(tempMapTiles[tileY][tileX].getTileID());
                        }
                    } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                        MapTile[][] tempMapTiles = gameState.getCurrentMap().getMapTiles();
                        if(DEBUG_OUTPUT) {
                            System.out.println("TILEID: " + tempMapTiles[tileY][tileX].getTileID());
                        }
                        tempMapTiles[tileY][tileX].setTileID(gameState.getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTiles().length - 1);
                        tempMapTiles[tileY][tileX].setTileSet(devMenu.SELECTED_TILE_SET_ID);
                        gameState.getCurrentMap().setMapTiles(tempMapTiles);
                    }
                } else {
                    // Handle Mouse Drag
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
                        } else if (releasedX <= DRAG_LOC[0] && releasedY <= DRAG_LOC[1]) {
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
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1);
                                }
                            }
                        } else if (releasedY <= DRAG_LOC[1] && !(releasedX <= DRAG_LOC[0])) {
                            for (int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for (int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1);
                                }
                            }
                        } else if (releasedX <= DRAG_LOC[0] && releasedY <= DRAG_LOC[1]) {
                            for (int y = releasedY; y < DRAG_LOC[1] + 1; y++) {
                                for (int x = releasedX; x < DRAG_LOC[0] + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1);
                                }
                            }
                        } else {
                            for (int y = DRAG_LOC[1]; y < releasedY + 1; y++) {
                                for (int x = DRAG_LOC[0]; x < releasedX + 1; x++) {
                                    MapTile[][] tempMap = gameState.getCurrentMap().getMapTiles();
                                    tempMap[y][x].setTileID(gameState.getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTotalTiles() - 1);
                                }
                            }
                        }
                    }
                    // Reset Drag Values
                    DRAG_LOC[0] = -1;
                    DRAG_LOC[1] = -1;
                }
            } else if(gameState == null || gameState.getCurrentState() == GameState.STATE.MAIN_MENU) {
                // If no gameState exists or gameState is currently in MAIN_MENU mode
                // We take the mouse click and see if it is in the same space as the "Play" label
                int xLoc = menuNewGameBounds[0]; // (x,y) Location is the upper left of the rectangle
                int yLoc = menuNewGameBounds[1];
                int xWidth = menuNewGameBounds[2]; // (x,y) Width/Height is the pixel width and height of the label
                int yHeight = menuNewGameBounds[3];
                if(mouseX >= xLoc && mouseX <= xWidth + xLoc) {
                    if(mouseY <= yLoc && mouseY >= yLoc - yHeight) {
                        newGame(); // If clicked start a new game
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.GAME) {
                int[] tileXY = { mouseX / TILE_SIZE, mouseY / TILE_SIZE }; // Gets the ( x, y ) of a tile
                for(Entity e: gameState.getEntities()) {
                    if(e instanceof Character) { // Only check characters
                        int charX = ((Character) e).getX(); // get their x and y
                        int charY = ((Character) e).getY();
                        if(charX == tileXY[0] && charY == tileXY[1]) {
                            // If it is equal you've pressed the tile that that character occupies
                            lastSelectedCharUID = e.getUID();
                            if(DEBUG_OUTPUT) {
                                System.out.println("You've clicked a Character");
                                System.out.println("getName returns: " + ((Character) e).getName());
                                System.out.println("isMoveTurn returns: " + ((Character) e).isMoveTurn());
                                System.out.println("isAlive returns: " + ((Character) e).isAlive());
                                System.out.println("getHp returns: " + ((Character)e).getHp());
                                System.out.println("getAttack returns: " + ((Character)e).getAttack());
                                System.out.println("getCritical returns: " + ((Character)e).getCritical());
                                System.out.println("getDefense returns: " + ((Character)e).getDefense());
                                System.out.println("getCharClass.getLevel returns: " + ((Character) e).getCharClass().getLevel());
                                System.out.println("getCharClass.getCurrentXP returns: " + ((Character) e).getCharClass().getCurrentXP());
                            }
                        }
                    }
                }
            } else if(gameState.getCurrentState() == GameState.STATE.BATTLE) {
                // DEFEND BUTTON
                final int defendX = (SCREEN_WIDTH>>1)+(SCREEN_WIDTH>>4);
                final int defendY = (SCREEN_MAP_HEIGHT>>1)+(SCREEN_MAP_HEIGHT>>3)+(SCREEN_MAP_HEIGHT>>4);
                final int defendW = (SCREEN_WIDTH>>2)+(SCREEN_WIDTH>>3);
                final int defendH = SCREEN_MAP_HEIGHT>>2;
                if(mouseX >= defendX && mouseX <= defendX + defendW) {
                    // within x bounds
                    if(mouseY >= defendY && mouseY <= defendY + defendH) {
                        // also within y bounds
                        if(DEBUG_OUTPUT) {
                            System.out.println("Defend Button Clicked");
                        }
                    }
                }
                // ATTACK BUTTON
                final int attackX = SCREEN_WIDTH>>4;
                final int attackY = defendY; // Same code line
                final int attackW = (SCREEN_WIDTH>>2)+(SCREEN_WIDTH>>3);
                final int attackH = SCREEN_MAP_HEIGHT>>2;
                if(mouseX >= attackX && mouseX <= attackX + attackW) {
                    // within x bounds
                    if(mouseY >= attackY && mouseY <= mouseY + attackH) {
                        // within y bounds

                        // activate animation
                        if(gameState.getPlayerEntity().isBattleTurn()) {
                            gameState.getPlayerEntity().setIsAttacking(true);
                        }
                        if(DEBUG_OUTPUT) {
                            System.out.println("Attack Button Clicked");
                        }
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
                        // within x bounds
                        if(mouseY >= xy[1] && mouseY <= xy[1] + squareSize) {
                            // within y bounds
                            if(count < gameState.getMaps().size()) {
                                // sets current map
                                gameState.setCurrentMap(gameState.getMaps().get(count));
                            }
                        }
                    }
                    count++;
                }
            }
        });
        // Game screen mouse drag handler, only used for editMode
        canvas.addEventHandler(MouseDragEvent.DRAG_DETECTED, (mouseEvent) -> {
            // Captures the tile x,y of the start of a mouse drag
            if(gameState != null && devMenu.EDIT_MODE && gameState.getCurrentState() == GameState.STATE.GAME) {
                DRAG_LOC[0] = (int) mouseEvent.getSceneX() / gameState.getCurrentMap().getTileSize();
                DRAG_LOC[1] = (int) mouseEvent.getSceneY() / gameState.getCurrentMap().getTileSize();

                //DRAG_LOC[0] += gameState.getCurrentMap().getXOffset();
                //DRAG_LOC[1] += gameState.getCurrentMap().getYOffset();
            }
        });
        // Show the window after it's fully initialized
        primaryStage.setScene(rootScene);
        primaryStage.show();
        // GAME LOOP, we use a lambda to override the handle() method of the animator
        // Update then render and only every 1/30 of a second
        // Animation timer tries to go for 1/60 or 60fps but we cap it
        // Does a lot of the leg work needed to keep things running smoothly
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
    // Pulls all file names from a given directory and returns a String[] of the names including .dat .png etc
    private static String[] getFileNamesFromDirectory(String path) {
        // String path must be a windows directory, it can be an absolute path or relative path to the .jar/.class files
        File[] files;
        String[] fileNames = new String[0];
        try {
            files = new File(path).listFiles();
            if(files != null) {
                fileNames = new String[files.length];
                for(int i=0; i<files.length; i++) {
                    if(files[i].isFile()) {
                        fileNames[i] = files[i].getName();
                    }
                }
            }

        } catch (Exception e) {
            // Need better error handling here, this indicates a significant flaw in file structure
            e.printStackTrace();
        }
        return fileNames;
    }
    public static int[] getMapDimensions() {
        int[] result = new int[2];
        result[0] = SCREEN_WIDTH/TILE_SIZE;
        result[1] = SCREEN_MAP_HEIGHT/TILE_SIZE;
        return result;
    }
    public String getProgramVersion() { return PROGRAM_VERSION; }
    public GameState getGameState() {
        return gameState;
    }
    public int getLastSelectChar () { return lastSelectedCharUID;}
    // Run the JavaFX program, this will call the start() method and give it a Stage object to use as primary stage
    public static void main(String[] args) {
        String[] classPath = System.getProperty("java.class.path").split(";");
        if(!classPath[0].equals("")) {
            GAME_DATA_PATH = classPath[0] + "/GameData";
        } else {
            GAME_DATA_PATH = Paths.get("").toAbsolutePath().toString() + "/GameData";
        }
        launch(args);
    }
}