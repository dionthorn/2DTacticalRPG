package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;

/**
 * The NPC class creates Character objects but adds the needed AI logic for NPCs
 */
public class NonPlayerCharacter extends Character {

    private Dice dice = new Dice(2);
    private int collisionCounter = 0;

    /**
     * NPC Default Constructor will assign all needed superclass requirements
     * @param map the assigned map for the NPC
     * @param spritePath the path to the NPC spritesheet
     * @param name the name of the NPC
     * @param x the default x location of the NPC
     * @param y the default y location of the NPC
     * @param charClass the default CharacterClass of the NPC
     */
    public NonPlayerCharacter(Map map, String spritePath, String name, int x, int y, CharacterClass charClass) {
        super(map, spritePath, name, x, y, charClass);
    }

    /**
     * NPC AI random movement logic will attempt 10 random directions if no solution is found it will end turn
     * @param gameState used to determine where other entities are for NPC movement.
     */
    public void moveRandom(GameState gameState) {
        int direction = dice.roll() - 1;
        int posORneg = dice.roll() - 1;
        if (direction == 0) {
            if (posORneg == 0) {
                Character target = checkEnemyCollision(gameState, this.x + 1, this.y);
                if (target == null) {
                    if(checkFriendlyCollision(gameState, this.x + 1, this.y)) {
                        collisionCounter++;
                        this.update(gameState);
                    } else {
                        move(1, 0);
                    }
                } else {
                    if(target.isAlive()) {
                        gameState.startBattle(this, target);
                    } else {
                        collisionCounter++;
                        this.update(gameState);
                    }
                }
            } else {
                Character target = checkEnemyCollision(gameState, this.x - 1, this.y);
                if (target == null) {
                    if(checkFriendlyCollision(gameState, this.x - 1, this.y)) {
                        collisionCounter++;
                        this.update(gameState);
                    } else {
                        move(-1, 0);
                    }
                } else {
                    if(target.isAlive()) {
                        gameState.startBattle(this, target);
                    } else {
                        collisionCounter++;
                        this.update(gameState);
                    }
                }
            }
        } else {
            if (posORneg == 0) {
                Character target = checkEnemyCollision(gameState, this.x, this.y + 1);
                if (target == null) {
                    if(checkFriendlyCollision(gameState, this.x, this.y + 1)) {
                        collisionCounter++;
                        this.update(gameState);
                    } else {
                        move(0, 1);
                    }
                } else {
                    if(target.isAlive()) {
                        gameState.startBattle(this, target);
                    } else {
                        collisionCounter++;
                        this.update(gameState);
                    }
                }
            } else {
                Character target = checkEnemyCollision(gameState, this.x, this.y - 1);
                if (target == null) {
                    if(checkFriendlyCollision(gameState, this.x, this.y - 1)) {
                        collisionCounter++;
                        this.update(gameState);
                    } else {
                        move(0, -1);
                    }
                } else {
                    if(target.isAlive()) {
                        gameState.startBattle(this, target);
                    } else {
                        collisionCounter++;
                        this.update(gameState);
                    }
                }
            }
        }
    }

    /**
     * Draws the NPC to the Canvas
     * @param gc the graphics context is used to draw onto the canvas object during gameplay
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.drawImage(getCurrentSprite(), getX() * tileSize, getY() * tileSize,32,32);
    }

    /**
     * When an NPC is updated it will perform its AI functions
     * @param gameState the current gameState used to update the entity
     */
    @Override
    public void update(GameState gameState) {
        if(isMoveTurn && gameState.getNextTurn()) {
            if(isAlive && collisionCounter < 10) {
                boolean playerTeamCheck = false;
                for(Entity e: gameState.getPlayerTeam()) {
                    if (e.equals(this)) {
                        playerTeamCheck = true;
                        break;
                    }
                }
                boolean onSameY = false;
                boolean posX = false;
                if(playerTeamCheck) {
                    for(Entity e: gameState.getEnemyTeam()) {
                        if(((PhysicalEntity) e).getY() == this.getY()) {
                            if(((Character) e).isAlive()) {
                                onSameY = true;
                                if(((PhysicalEntity) e).getX() > this.getX()) {
                                    posX = true;
                                }
                            }
                        }
                    }
                } else {
                    for(Entity e: gameState.getPlayerTeam()) {
                        if(((PhysicalEntity) e).getY() == this.getY()) {
                            if(((Character) e).isAlive()) {
                                onSameY = true;
                                if (((PhysicalEntity) e).getX() > this.getX()) {
                                    posX = true;
                                }
                            }
                        }
                    }
                }
                if(onSameY) {
                    if(posX) {
                        Character target = checkEnemyCollision(gameState, this.x + 1, this.y);
                        if (target == null) {
                            if(checkFriendlyCollision(gameState, this.x + 1, this.y)) {
                                moveRandom(gameState);
                            } else {
                                move(1, 0);
                            }
                        } else {
                            if(target.isAlive()) {
                                gameState.startBattle(this, target);
                            } else {
                                moveRandom(gameState);
                            }
                        }
                    } else {
                        Character target = checkEnemyCollision(gameState, this.x - 1, this.y);
                        if (target == null) {
                            if(checkFriendlyCollision(gameState, this.x - 1, this.y)) {
                                moveRandom(gameState);
                            } else {
                                move(-1, 0);
                            }
                        } else {
                            if(target.isAlive()) {
                                gameState.startBattle(this, target);
                            } else {
                                moveRandom(gameState);
                            }
                        }
                    }
                } else {
                    moveRandom(gameState);
                }
            } else if(collisionCounter >= 10) {
                if(Run.DEBUG_OUTPUT) {
                    System.out.println("No AI movement solution found over 10 random tries");
                }
                collisionCounter = 0;
            }
            if(gameState.getPlayerTeam().contains(this)) {
                boolean found = false;
                for(Entity e: gameState.getPlayerTeam()) {
                    if(e.getUID() == getUID()) {
                        found = true;
                    } else if(found) {
                        ((Character) e).setMoveTurn(true);
                        found = false;
                    }
                }
                if(found) {
                    ((Character) gameState.getEnemyTeam().get(0)).setMoveTurn(true);
                }
            } else if(gameState.getEnemyTeam().contains(this)) {
                boolean found = false;
                for(Entity e: gameState.getEnemyTeam()) {
                    if(e.getUID() == getUID()) {
                        found = true;
                    } else if(found) {
                        ((Character) e).setMoveTurn(true);
                        found = false;
                    }
                }
                if(found) {
                    ((Character) gameState.getPlayerTeam().get(0)).setMoveTurn(true);
                }
            }
            isMoveTurn = false;
            gameState.setNextTurn(false);
        }
    }
}

