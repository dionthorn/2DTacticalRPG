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

public class Render {

    private static final Image mainMenuBg = new Image("file:" + Run.GAME_ART_PATH + File.separator + "main_menu.png");
    private static final Image paperBg = new Image("file:" + Run.GAME_ART_PATH + File.separator + "paper.png",
            Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT-Run.SCREEN_MAP_HEIGHT, false, false
    );
    private static int battleFrameCounter = 0;
    public static int[] menuNewGameBounds;

    public static void render(Run app, DevMenu devMenu, GraphicsContext gc) {

        if (app.getGameState() != null && devMenu != null) {
            Image selectedTileImg = app.getGameState().getCurrentMap().getTile(devMenu.SELECTED_TILE_SET_ID,
                    devMenu.SELECTED_TILE_ID
            );
            ImageView selectedTileImgView = new ImageView(selectedTileImg);
            devMenu.getDevMenu().getChildren().add(selectedTileImgView);
            GridPane.setConstraints(selectedTileImgView, 4, 0);
            devMenu.getDevMenu().getChildren().remove(devMenu.getTileSetView());
            Image tileSet = app.getGameState().getCurrentMap().getTileSet(devMenu.SELECTED_TILE_SET_ID).getTileSetSrc();
            devMenu.setTileSetView(new ImageView(tileSet));
            devMenu.getDevMenu().getChildren().add(devMenu.getTileSetView());
            GridPane.setConstraints(devMenu.getTileSetView(),
                    0, 2,
                    32, 1
            );
        }
        if (app.getGameState() == null || app.getGameState().getCurrentState() == GameState.STATE.MAIN_MENU) {
            gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
            gc.drawImage(mainMenuBg, 0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
            gc.setTextAlign(TextAlignment.LEFT);
            Font menuTitleFont = new Font("Arial", 32);
            gc.setFont(menuTitleFont);
            gc.setFill(Color.WHITE);
            String title = "Game Project";
            Text menuTitle = new Text(title);
            menuTitle.setFont(menuTitleFont);
            gc.fillText(title, (Run.SCREEN_WIDTH >> 1) - (menuTitle.getLayoutBounds().getWidth() / 2),
                    Run.SCREEN_HEIGHT >> 4
            );
            Font menuOptionsFont = new Font("Arial", 28);
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
        } else if (app.getGameState() != null && app.getGameState().getCurrentState() == GameState.STATE.GAME) {
            gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT); // Clear canvas
            MapTile[][] mapTiles = app.getGameState().getCurrentMap().getMapTiles();
            for (int y = 0; y < mapTiles.length; y++) {
                for (int x = 0; x < mapTiles[0].length; x++) {
                    gc.drawImage(app.getGameState().getCurrentMap().getTile(mapTiles[y][x].getTileSet(),
                            mapTiles[y][x].getTileID()),
                            x * app.getGameState().getCurrentMap().getTileSize(),
                            y * app.getGameState().getCurrentMap().getTileSize()
                    );
                }
            }
            gc.drawImage(paperBg, 0, app.getGameState().getCurrentMap().getMapHeight() * Run.TILE_SIZE);
            for (Entity e : app.getGameState().getEntities()) {
                if (e instanceof Drawable) {
                    if (((PhysicalEntity) e).getCurrentMap().equals(app.getGameState().getCurrentMap())) {
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
                            pixelPerHP = maxHP / Run.TILE_SIZE;
                            currentHP = ((Character) e).getHp();
                            currentHPDisplayed = currentHP / pixelPerHP;
                            YaxisMod = (y * Run.TILE_SIZE) + ((maxHP - currentHP) / pixelPerHP);
                            gc.setFill(Color.RED);
                            gc.setStroke(Color.BLACK);
                            if (app.getGameState().getPlayerTeam().contains(e)) {
                                gc.fillRect(x * Run.TILE_SIZE, y * Run.TILE_SIZE, 3, 32);
                                gc.setFill(Color.GREEN);
                                gc.setStroke(Color.BLACK);
                                if (currentHPDisplayed < 0) {
                                    currentHPDisplayed = 0;
                                }
                                gc.fillRect(x * Run.TILE_SIZE, YaxisMod, 3, currentHPDisplayed);
                            } else if (app.getGameState().getEnemyTeam().contains(e)) {
                                gc.fillRect((x * Run.TILE_SIZE) + 29, y * Run.TILE_SIZE, 3, 32);
                                gc.setFill(Color.GREEN);
                                gc.setStroke(Color.BLACK);
                                if (currentHPDisplayed < 0) {
                                    currentHPDisplayed = 0;
                                }
                                gc.fillRect((x * Run.TILE_SIZE) + 29, YaxisMod, 3, currentHPDisplayed);
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
        } else if (app.getGameState() != null && app.getGameState().getCurrentState() == GameState.STATE.BATTLE) {
            gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
            gc.setFill(Color.rgb(43, 107, 140));
            gc.fillRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_MAP_HEIGHT);
            gc.drawImage(paperBg, 0, Run.SCREEN_MAP_HEIGHT);
            gc.setFill(Color.GREY);
            int stageX, stageY, stageW, stageH;
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
            gc.fillText(heightTest, 50, Run.SCREEN_MAP_HEIGHT + textHeight + 5);
            gc.fillText(String.format("HP: %.0f / %.0f", ally.getHp(), ally.getMaxHP()),
                    50, Run.SCREEN_MAP_HEIGHT + textHeight * 3
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
            gc.fillText(String.format("Name: %s", enemy.getName()), (Run.SCREEN_WIDTH >> 1) + 50,
                    Run.SCREEN_MAP_HEIGHT + textHeight + 5
            );
            gc.fillText(String.format("HP: %.0f / %.0f", enemy.getHp(), enemy.getMaxHP()),
                    (Run.SCREEN_WIDTH >> 1) + 50, Run.SCREEN_MAP_HEIGHT + textHeight * 3
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
                                app.getGameState().setState(GameState.STATE.GAME);
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
                if (enemy.IsAttacking()) {
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
        } else if(app.getGameState() != null && app.getGameState().getCurrentState() == GameState.STATE.LEVEL_SELECTION) {
            gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
            gc.drawImage(mainMenuBg, 0, 0, Run.SCREEN_WIDTH, Run.SCREEN_MAP_HEIGHT);
            gc.setFill(Color.rgb(43, 107, 140));
            int squareSize = 200;
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
            gc.setFont(new Font(12));
            for(Map m: app.getGameState().getMaps()) {
                String[] path = m.getPATH().split("\\\\");
                int index;
                index = Math.max((path.length - 1), 0);
                String iconName = path[index].split("\\.")[0] + "_Icon.png";
                if(FileOps.doesFileExist(Run.GAME_ART_PATH + File.separator + iconName)) {
                    app.getGameState().getMaps().get(count).setIcon(new Image("file:" + Run.GAME_ART_PATH + File.separator + iconName));
                    gc.drawImage(app.getGameState().getMaps().get(count).getIcon(), squareXY[count][0], squareXY[count][1]);
                } else {
                    gc.fillText(path[index], squareXY[count][0]+10, squareXY[count][1]+10);
                }
                count++;
            }
            gc.drawImage(paperBg, 0, Run.SCREEN_MAP_HEIGHT);
            gc.setFont(new Font("Arial", 32));
            String[] path = app.getGameState().getCurrentMap().getPATH().split("\\\\");
            gc.fillText(path[path.length - 1], Run.SCREEN_WIDTH>>4, Run.SCREEN_MAP_HEIGHT + (Run.SCREEN_MAP_HEIGHT>>4));
            gc.fillText("Press Enter to Load The Selected Map", Run.SCREEN_WIDTH>>4,
                    Run.SCREEN_MAP_HEIGHT + (Run.SCREEN_MAP_HEIGHT>>2)
            );
        } else if(app.getGameState() != null && app.getGameState().getCurrentState() == GameState.STATE.GAME_OVER) {
            gc.clearRect(0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
            gc.drawImage(mainMenuBg, 0, 0, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            Text gameOver = new Text("GAME OVER!");
            gc.fillText(gameOver.getText(), Run.SCREEN_WIDTH >> 1, 40);
            gc.fillText("Press ESC to exit to Main Menu!", Run.SCREEN_WIDTH >> 1, 100);
        }
    }

}
