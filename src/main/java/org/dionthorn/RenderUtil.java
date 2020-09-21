package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.File;

public class RenderUtil {

    private static final Image mainMenuBg = new Image("file:" + Run.GAME_ART_PATH + File.separator + "main_menu.png");
    private static final Image paperBg = new Image("file:" + Run.GAME_ART_PATH + File.separator + "paper.png",
            Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT-Run.SCREEN_MAP_HEIGHT, false, false
    );
    private static int battleFrameCounter = 0;
    public static int[] menuNewGameBounds;
    public static int[] anchorUL = new int[2]; // the XY of tile that would be in the upper left of view area
    private static final Font smallFont = new Font("Arial", 12);
    private static final Font mediumFont = new Font("Arial", 28);
    private static final Font largeFont = new Font("Arial", 32);

    public static void render(Run app, DevMenu devMenu, GraphicsContext gc) {
        if (app.getGameState() != null && devMenu != null) {
            Image selectedTileImg = app.getGameState().getCurrentMap().getTile(devMenu.SELECTED_TILE_SET_ID,
                    devMenu.SELECTED_TILE_ID
            );
            ImageView selectedTileImgView = new ImageView(selectedTileImg);
            devMenu.getDevMenu().getChildren().add(selectedTileImgView);
            GridPane.setConstraints(selectedTileImgView, 3, 9);
            devMenu.getDevMenu().getChildren().remove(devMenu.getTileSetView());
            Image tileSet = app.getGameState().getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTileSetSrc();
            devMenu.setTileSetView(new ImageView(tileSet));
            devMenu.getDevMenu().getChildren().add(devMenu.getTileSetView());
            GridPane.setConstraints(devMenu.getTileSetView(),
                    0, 10,
                    32, 1
            );
        }
        if (app.getGameState() == null || app.getGameState().getCurrentState() == GameState.STATE.MAIN_MENU) {
            drawMainMenu(gc);
        } else if (app.getGameState() != null && app.getGameState().getCurrentState() == GameState.STATE.GAME) {
            drawGameScreen(app, gc);
        } else if (app.getGameState() != null && app.getGameState().getCurrentState() == GameState.STATE.BATTLE) {
            drawBattleScreen(app, gc);
        } else if(app.getGameState() != null && app.getGameState().getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
            drawLevelSelectionScreen(app, gc);
        } else if(app.getGameState() != null && app.getGameState().getCurrentState() == GameState.STATE.GAME_OVER) {
            drawGameOverScreen(gc);
        }
    }

    public static void drawMainMenu(GraphicsContext gc) {
        gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
        gc.drawImage(mainMenuBg, 0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
        gc.setTextAlign(TextAlignment.LEFT);
        Font menuTitleFont = largeFont;
        gc.setFont(menuTitleFont);
        gc.setFill(Color.WHITE);
        String title = "Game Project";
        Text menuTitle = new Text(title);
        menuTitle.setFont(menuTitleFont);
        gc.fillText(title, (Run.SCREEN_WIDTH >> 1) - (menuTitle.getLayoutBounds().getWidth() / 2),
                Run.SCREEN_HEIGHT >> 4
        );
        Font menuOptionsFont = mediumFont;
        gc.setFont(menuOptionsFont);
        String newGameString = "Play";
        Text newGameText = new Text(newGameString);
        newGameText.setFont(menuOptionsFont);
        menuNewGameBounds = new int[4];
        menuNewGameBounds[0] = (int) ((Run.SCREEN_WIDTH >> 1) - (newGameText.getLayoutBounds().getWidth() / 2));
        menuNewGameBounds[1] = (Run.SCREEN_HEIGHT >> 4) * 4;
        menuNewGameBounds[2] = (int) newGameText.getLayoutBounds().getWidth();
        menuNewGameBounds[3] = (int) newGameText.getLayoutBounds().getHeight();
        gc.fillText(newGameString, menuNewGameBounds[0], menuNewGameBounds[1]);
    }

    public static void drawGameScreen(Run app, GraphicsContext gc) {
        gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT); // Clear canvas
        MapTile[][] mapTiles = app.getGameState().getCurrentMap().getMapTiles();
        int[] mapAreaXY = Run.getMapAreaDimensions();
        // System.out.println("mapAreaDimensions: (" + mapAreaXY[0] + ", " + mapAreaXY[1] + ")");
        int[] currentMapXY = {
                app.getGameState().getCurrentMap().getMapWidth(),
                app.getGameState().getCurrentMap().getMapHeight()
        };
        // System.out.println("currentMapDimensions: (" + currentMapXY[0] + ", " + currentMapXY[1] + ")");
        // now I have two squares whose width and height are in [width, height] format.
        int[] playerXY = {
                app.getGameState().getPlayerEntity().getX(),
                app.getGameState().getPlayerEntity().getY()
        };
        // System.out.println("player location: (" + playerXY[0] + ", " + playerXY[1] + ")");
        anchorUL[0] = 0;
        anchorUL[1] = 0;
        int howFarPastMiddle;
        if(playerXY[1] > (mapAreaXY[1] / 2)) {
            // if player is beyond half of the rendered area then we need to shift.
            anchorUL[1] = (mapAreaXY[1] / 2);
            howFarPastMiddle = playerXY[1] - mapAreaXY[1];
            anchorUL[1] += howFarPastMiddle;
        }
        if(playerXY[0] > (mapAreaXY[0] / 2)) {
            anchorUL[0] = (mapAreaXY[0] / 2);
            howFarPastMiddle = playerXY[0] - mapAreaXY[0];
            anchorUL[0] += howFarPastMiddle;
        }
        // System.out.println("anchorXY: (" + anchorUL[0] + ", " + anchorUL[1] + ")");
        // Render all the correct mapTiles to the correct relative locations.
        if(mapAreaXY[1] >= currentMapXY[1] && mapAreaXY[0] >= currentMapXY[0]) {
            // this means we don't need to use the anchor for this map
            anchorUL[0] = 0;
            anchorUL[1] = 0;
        }
        for(int y = 0; y < mapAreaXY[1]; y++) {
            for(int x = 0; x < mapAreaXY[0]; x++) {
                int yOffset = Math.abs(anchorUL[1]);
                int xOffset = Math.abs(anchorUL[0]);
                if(y + yOffset < 0 || x + xOffset < 0) {
                    gc.drawImage(app.getGameState().getCurrentMap().getTileSet(0).getBlank(),
                            x * Run.TILE_SIZE,
                            y * Run.TILE_SIZE
                    );
                } else if(y + yOffset >= currentMapXY[1] || x + xOffset >= currentMapXY[0]) {
                    gc.drawImage(app.getGameState().getCurrentMap().getTileSet(0).getBlank(),
                            x * Run.TILE_SIZE,
                            y * Run.TILE_SIZE
                    );
                } else {
                    gc.drawImage(app.getGameState().getCurrentMap().getTile(
                            mapTiles[y + yOffset][x + xOffset].getTileSet(),
                            mapTiles[y + yOffset][x + xOffset].getTileID()
                            ),
                            x * Run.TILE_SIZE,
                            y * Run.TILE_SIZE
                    );
                }

            }
        }
        // Draw UI prompt area below map area here
        gc.drawImage(paperBg, 0, Run.SCREEN_MAP_HEIGHT);
        // Draw all entities
        for (Entity e : app.getGameState().getEntities()) {
            if (e instanceof Drawable) {
                if (((PhysicalEntity) e).getCurrentMap().equals(app.getGameState().getCurrentMap())) {
                    if (e instanceof Character && !((Character) e).isAlive()) {
                        ((Character) e).setCurrentSprite(((Character) e).getCharClass().getDeadTileID());
                    }
                    if (e instanceof Character) {
                        double x;
                        double y;
                        double maxHP;
                        double pixelPerHP;
                        double currentHP;
                        double currentHPDisplayed;
                        double YaxisMod;
                        x = ((Character) e).getX();
                        y = ((Character) e).getY();
                        double relX = ((Character) e).getRealtiveX();
                        double relY = ((Character) e).getRealtiveY();
                        // check bounds of anchor map area
                        if((y < mapAreaXY[1] + anchorUL[1] && y >= anchorUL[1]) &&
                                (x < mapAreaXY[0] + anchorUL[0] && x >= anchorUL[0])) {
                            ((Drawable) e).draw(gc);
                            maxHP = ((Character) e).getMaxHP();
                            pixelPerHP = maxHP / Run.TILE_SIZE;
                            currentHP = ((Character) e).getHp();
                            currentHPDisplayed = currentHP / pixelPerHP;
                            YaxisMod = (relY * Run.TILE_SIZE) + ((maxHP - currentHP) / pixelPerHP);
                            gc.setFill(Color.RED);
                            gc.setStroke(Color.BLACK);
                            if (app.getGameState().getPlayerTeam().contains(e)) {
                                gc.fillRect(relX * Run.TILE_SIZE, relY * Run.TILE_SIZE, 3, 32);
                                gc.setFill(Color.GREEN);
                                gc.setStroke(Color.BLACK);
                                if (currentHPDisplayed < 0) {
                                    currentHPDisplayed = 0;
                                }
                                gc.fillRect(relX * Run.TILE_SIZE, YaxisMod, 3, currentHPDisplayed);
                            } else if (app.getGameState().getEnemyTeam().contains(e)) {
                                gc.fillRect((relX * Run.TILE_SIZE) + 29, relY * Run.TILE_SIZE, 3, 32);
                                gc.setFill(Color.GREEN);
                                gc.setStroke(Color.BLACK);
                                if (currentHPDisplayed < 0) {
                                    currentHPDisplayed = 0;
                                }
                                gc.fillRect((relX * Run.TILE_SIZE) + 29, YaxisMod, 3, currentHPDisplayed);
                            }
                        }
                    }
                }
            }
        }
        if (!app.getGameState().getNextTurn()) {
            String name = "";
            Entity target = null;
            for (Entity e : app.getGameState().getEnemyTeam()) {
                if (((Character) e).isMoveTurn()) {
                    name = ((Character) e).getName();
                    target = e;
                }
            }
            for (Entity e : app.getGameState().getPlayerTeam()) {
                if (((Character) e).isMoveTurn()) {
                    name = ((Character) e).getName();
                    target = e;
                }
            }
            if (!name.equals("")) {
                if (name.equals(app.getGameState().getPlayerEntity().getName())) {
                    gc.setStroke(Color.WHITE);
                    gc.setFill(Color.BLACK);
                    gc.setFont(mediumFont);
                    if (app.getGameState().getPlayerEntity().isAlive()) {
                        gc.setTextAlign(TextAlignment.LEFT);
                        gc.fillText("Please Take Your Turn", 10, Run.SCREEN_HEIGHT - 20);
                    } else {
                        app.getGameState().setState(GameState.STATE.GAME_OVER);
                    }
                } else {
                    if (((Character) target).isAlive()) {
                        gc.setStroke(Color.WHITE);
                        gc.setFill(Color.BLACK);
                        gc.setTextAlign(TextAlignment.LEFT);
                        gc.fillText(name + " Please Press Spacebar To Advance Their Turn",
                                10, Run.SCREEN_HEIGHT - 20);
                    } else {
                        if (!app.getGameState().getNextTurn() && !app.getGameState().getPlayerEntity().isMoveTurn()) {
                            app.getGameState().setNextTurn(true);
                        }
                    }
                }
            }
        }
    }

    public static void drawBattleScreen(Run app, GraphicsContext gc) {
        gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
        gc.setFill(Color.rgb(43, 107, 140));
        gc.fillRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_MAP_HEIGHT);
        gc.drawImage(paperBg, 0, Run.SCREEN_MAP_HEIGHT);
        gc.setFill(Color.GREY);
        int stageX;
        int stageY;
        int stageW;
        int stageH;
        gc.fillRect(stageX = Run.SCREEN_WIDTH >> 5, stageY = Run.SCREEN_MAP_HEIGHT >> 5,
                stageW = (Run.SCREEN_WIDTH - (Run.SCREEN_WIDTH >> 4)),
                stageH = (Run.SCREEN_MAP_HEIGHT >> 1) + (Run.SCREEN_MAP_HEIGHT >> 3)
        );
        Character ally = app.getGameState().getAttacker();
        Character enemy = app.getGameState().getDefender();
        for (Entity c : app.getGameState().getEnemyTeam()) {
            if (c == app.getGameState().getAttacker()) {
                ally = app.getGameState().getDefender();
                enemy = app.getGameState().getAttacker();
                break;
            }
        }
        int allyX;
        int allyY;
        int spriteSize = stageW >> 2;
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
        int xSpacingAlly = 50;
        int xSpacingEnemy = (Run.SCREEN_WIDTH >> 1) + xSpacingAlly;
        gc.fillRect(stageW / 6f, yAxisMod, stageW >> 2, 10);
        gc.setFill(Color.GREEN);
        gc.fillRect(stageW / 6f, yAxisMod, currentHPDisplayed, 10);
        gc.setFill(Color.BLACK);
        gc.setFont(mediumFont);
        gc.setTextAlign(TextAlignment.LEFT);
        String heightTest = String.format("Name: %s", ally.getName());
        double textHeight = new Text(heightTest).getBoundsInLocal().getHeight() + 5;
        gc.fillText(heightTest,
                xSpacingAlly,
                Run.SCREEN_MAP_HEIGHT + (textHeight + 5)
        );
        gc.fillText(String.format("LVL: %d", ally.getCharClass().getLevel()),
                xSpacingAlly,
                Run.SCREEN_MAP_HEIGHT + (textHeight * 3)
        );
        gc.fillText(String.format("HP: %.0f / %.0f", ally.getHp(), ally.getMaxHP()),
                xSpacingAlly,
                Run.SCREEN_MAP_HEIGHT + (textHeight * 4) + 5
        );
        gc.fillText(String.format("Last Roll: %d", ally.getLastAttackRoll()),
                xSpacingAlly,
                Run.SCREEN_MAP_HEIGHT + (textHeight * 6)
        );
        int enemyX;
        int enemyY;
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
        gc.fillText(String.format("Name: %s", enemy.getName()),
                xSpacingEnemy,
                Run.SCREEN_MAP_HEIGHT + (textHeight + 5)
        );
        gc.fillText(String.format("LVL: %d", enemy.getCharClass().getLevel()),
                xSpacingEnemy,
                Run.SCREEN_MAP_HEIGHT + (textHeight * 3)
        );
        gc.fillText(String.format("HP: %.0f / %.0f", enemy.getHp(), enemy.getMaxHP()),
                xSpacingEnemy,
                Run.SCREEN_MAP_HEIGHT + (textHeight * 4) + 5
        );
        gc.fillText(String.format("Last Roll: %d", enemy.getLastAttackRoll()),
                xSpacingEnemy,
                Run.SCREEN_MAP_HEIGHT + (textHeight * 6)
        );
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(5);
        gc.strokeRect(Run.SCREEN_WIDTH >> 5, Run.SCREEN_MAP_HEIGHT >> 5,
                (Run.SCREEN_WIDTH - (Run.SCREEN_WIDTH >> 4)), (Run.SCREEN_MAP_HEIGHT >> 1) + (Run.SCREEN_MAP_HEIGHT >> 3)
        );
        gc.setFill(Color.GREY);
        gc.setLineWidth(2);
        final int y1 = (Run.SCREEN_MAP_HEIGHT >> 1) + (Run.SCREEN_MAP_HEIGHT >> 3) + (Run.SCREEN_MAP_HEIGHT >> 4);
        gc.fillRect(Run.SCREEN_WIDTH >> 4, y1,
                (Run.SCREEN_WIDTH >> 2) + (Run.SCREEN_WIDTH >> 3), Run.SCREEN_MAP_HEIGHT >> 2
        );
        gc.strokeRect(Run.SCREEN_WIDTH >> 4, y1,
                (Run.SCREEN_WIDTH >> 2) + (Run.SCREEN_WIDTH >> 3), Run.SCREEN_MAP_HEIGHT >> 2
        );
        gc.setTextAlign(TextAlignment.CENTER);
        final int y2 = (Run.SCREEN_MAP_HEIGHT >> 1) + (Run.SCREEN_MAP_HEIGHT >> 2) + (Run.SCREEN_MAP_HEIGHT >> 4);
        gc.strokeText("ATTACK", Run.SCREEN_WIDTH >> 2, y2);
        gc.fillRect((Run.SCREEN_WIDTH >> 1) + (Run.SCREEN_WIDTH >> 4), y1,
                (Run.SCREEN_WIDTH >> 2) + (Run.SCREEN_WIDTH >> 3), Run.SCREEN_MAP_HEIGHT >> 2
        );
        gc.strokeRect((Run.SCREEN_WIDTH >> 1) + (Run.SCREEN_WIDTH >> 4), y1,
                (Run.SCREEN_WIDTH >> 2) + (Run.SCREEN_WIDTH >> 3), Run.SCREEN_MAP_HEIGHT >> 2
        );
        gc.strokeText("DEFEND", Run.SCREEN_WIDTH - (Run.SCREEN_WIDTH >> 2), y2);
        battleFrameCounter++;
        if (ally.isBattleTurn()) {
            if (app.getGameState().getPlayerEntity().equals(ally)) {
                if (ally.isAttacking()) {
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
                            app.getGameState().setState(GameState.STATE.GAME);
                        }
                    }
                    ally.attackAnimation(battleFrameCounter);
                }
            } else {
                if (ally.isAttacking()) {
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
                            app.getGameState().setState(GameState.STATE.GAME);
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
            if (enemy.isAttacking()) {
                if (enemy.getCharClass().getCompletedCycles() > 2) {
                    enemy.setIsAttacking(false);
                    enemy.setBattleTurn(false);
                    ally.setBattleTurn(true);
                    enemy.attack(ally);
                    enemy.getCharClass().setCompletedCycles(0);
                    if (!ally.isAlive()) {
                        if (ally.equals(app.getGameState().getPlayerEntity())) {
                            app.getGameState().setState(GameState.STATE.GAME_OVER);
                        } else {
                            boolean didLevel = enemy.getCharClass().addXP(
                                    1000 * ally.getCharClass().getLevel()
                            );
                            if (didLevel) {
                                enemy.levelUp();
                            }
                            app.getGameState().setState(GameState.STATE.GAME);
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
        if (battleFrameCounter >= CharacterClass.animationLength) {
            battleFrameCounter = 0;
        }
    }

    public static void drawLevelSelectionScreen(Run app, GraphicsContext gc) {
        gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
        gc.drawImage(mainMenuBg, 0, 0, Run.SCREEN_WIDTH, Run.SCREEN_MAP_HEIGHT);
        gc.setFill(Color.rgb(43, 107, 140));
        int squareSize;
        if(Run.SCREEN_MAP_HEIGHT < 768) {
            squareSize = 100;
        } else {
            squareSize = 200;
        }
        int[][] squareXY = {
                {(Run.SCREEN_WIDTH>>4),         Run.SCREEN_HEIGHT>>5},
                {(Run.SCREEN_WIDTH>>4) * 6,     Run.SCREEN_HEIGHT>>5},
                {(Run.SCREEN_WIDTH>>4) * 11,    Run.SCREEN_HEIGHT>>5},
                {(Run.SCREEN_WIDTH>>4),         (Run.SCREEN_HEIGHT>>5) * 9},
                {(Run.SCREEN_WIDTH>>4) * 6,     (Run.SCREEN_HEIGHT>>5) * 9},
                {(Run.SCREEN_WIDTH>>4) * 11,    (Run.SCREEN_HEIGHT>>5) * 9},
                {(Run.SCREEN_WIDTH>>4),         (Run.SCREEN_HEIGHT>>5) * 17},
                {(Run.SCREEN_WIDTH>>4) * 6,     (Run.SCREEN_HEIGHT>>5) * 17},
                {(Run.SCREEN_WIDTH>>4) * 11,    (Run.SCREEN_HEIGHT>>5) * 17}
        };
        for(int[] xy: squareXY) {
            gc.fillRect(xy[0], xy[1], squareSize, squareSize);
        }
        int count = 0;
        gc.setFill(Color.BLACK);
        gc.setFont(smallFont);
        for(Map m: app.getGameState().getMaps()) {
            String[] path = m.getPATH().split("\\\\");
            int index;
            index = Math.max((path.length - 1), 0);
            String iconName = path[index].split("\\.")[0] + "_Icon.png";
            if(FileOpUtils.doesFileExist(Run.GAME_ART_PATH + File.separator + iconName)) {
                app.getGameState().getMaps().get(count).setIcon(new Image("file:" + Run.GAME_ART_PATH + File.separator + iconName));
                gc.drawImage(app.getGameState().getMaps().get(count).getIcon(),
                        squareXY[count][0], squareXY[count][1],
                        squareSize, squareSize
                );
            } else {
                gc.fillText(path[index], squareXY[count][0]+10, squareXY[count][1]+10);
            }
            count++;
        }
        gc.drawImage(paperBg, 0, Run.SCREEN_MAP_HEIGHT);
        gc.setFont(smallFont);
        String[] path = app.getGameState().getCurrentMap().getPATH().split("\\\\");
        gc.setFont(mediumFont);
        gc.fillText("Selected Map: " + path[path.length - 1], Run.SCREEN_WIDTH>>4, Run.SCREEN_MAP_HEIGHT + (Run.SCREEN_MAP_HEIGHT>>4));
        gc.fillText("Press Enter to Load The Selected Map", Run.SCREEN_WIDTH>>4,
                Run.SCREEN_MAP_HEIGHT + (Run.SCREEN_MAP_HEIGHT>>2)
        );
    }

    public static void drawGameOverScreen(GraphicsContext gc) {
        gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
        gc.drawImage(mainMenuBg, 0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(largeFont);
        gc.fillText("GAME OVER!", Run.SCREEN_WIDTH >> 1, 50);
        gc.setFont(mediumFont);
        gc.fillText("Press ESC to exit to Main Menu!", Run.SCREEN_WIDTH >> 1, 120);
    }

}
