package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;

public class NonPlayerCharacter extends Character {

    private Dice dice = new Dice(2);
    private int collisionCounter = 0;

    public NonPlayerCharacter(Map map, String spritePath, String name, int x, int y, CharacterClass charClass) {
        super(map, spritePath, name, x, y, charClass);
    }

    public void moveRandom(GameState gameState) {
        int direction = dice.roll() - 1; // 0 is x        1 is y
        int posORneg = dice.roll() - 1;  // 0 is positive 1 is negative
        if (direction == 0) {
            // x axis
            if (posORneg == 0) {
                // Positive
                Character target = checkEnemyCollision(gameState, this.x + 1, this.y);
                if (target == null) {
                    if(checkFriendlyCollision(gameState, this.x + 1, this.y)) {
                        collisionCounter++;
                        this.update(gameState); // Reroll
                    } else {
                        move(1, 0);
                    }
                } else {
                    if(target.isAlive()) {
                        gameState.startBattle(this, target);
                    } else {
                        collisionCounter++;
                        this.update(gameState); // Reroll
                    }
                }
            } else {
                // Negative
                Character target = checkEnemyCollision(gameState, this.x - 1, this.y);
                if (target == null) {
                    if(checkFriendlyCollision(gameState, this.x - 1, this.y)) {
                        collisionCounter++;
                        this.update(gameState); // Reroll
                    } else {
                        move(-1, 0);
                    }
                } else {
                    if(target.isAlive()) {
                        gameState.startBattle(this, target);
                    } else {
                        collisionCounter++;
                        this.update(gameState); // Reroll
                    }
                }
            }
        } else {
            // y axis
            if (posORneg == 0) {
                // Positive
                Character target = checkEnemyCollision(gameState, this.x, this.y + 1);
                if (target == null) {
                    if(checkFriendlyCollision(gameState, this.x, this.y + 1)) {
                        collisionCounter++;
                        this.update(gameState); // Reroll
                    } else {
                        move(0, 1);
                    }
                } else {
                    if(target.isAlive()) {
                        gameState.startBattle(this, target);
                    } else {
                        collisionCounter++;
                        this.update(gameState); // Reroll
                    }
                }
            } else {
                // Negative
                Character target = checkEnemyCollision(gameState, this.x, this.y - 1);
                if (target == null) {
                    if(checkFriendlyCollision(gameState, this.x, this.y - 1)) {
                        collisionCounter++;
                        this.update(gameState); // Reroll
                    } else {
                        move(0, -1);
                    }
                } else {
                    if(target.isAlive()) {
                        gameState.startBattle(this, target);
                    } else {
                        collisionCounter++;
                        this.update(gameState); // Reroll
                    }
                }
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Will be called by the JavaFX AnimationTimer each frame!
        // The gc can be used to draw onto the canvas
        gc.drawImage(getCurrentSprite(), getX() * tileSize, getY() * tileSize,32,32);
    }

    @Override
    public void update(GameState gameState) {
        // Will be called just before a draw() call, can be used to update in between frames
        if(isMoveTurn && gameState.getNextTurn()) {
            if(isAlive && collisionCounter < 10) {
                // New move logic, check if enemy is on same x axis, if true move towards, otherwise random move.
                boolean playerTeamCheck = false;
                for(Entity e: gameState.getPlayerTeam()) {
                    if(e.equals(this)) {
                        playerTeamCheck = true;
                    }
                }
                boolean onSameY = false; // if enemy on same Y axis
                boolean posX = false; // if enemy is in the pos x direction otherwise in neg x direction
                if(playerTeamCheck) {
                    // this entity is on player team so search enemy team
                    for(Entity e: gameState.getEnemyTeam()) {
                        if(((PhysicalEntity) e).getY() == this.getY()) {
                            // on same y axis
                            if(((Character) e).isAlive()) {
                                onSameY = true;
                                if(((PhysicalEntity) e).getX() > this.getX()) {
                                    posX = true;
                                }
                            }
                        }
                    }
                } else {
                    // this entity is on enemy team so search player team
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
                    // move toward enemy
                    if(posX) {
                        // move in posX direction
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
                        // move in negX direction
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
                    // If AI attempt isn't possible attempt to move randomly.
                    moveRandom(gameState);
                }
            } else if(collisionCounter >= 10) {
                if(Run.DEBUG_OUTPUT) {
                    System.out.println("No AI movement solution found over 10 random tries");
                }
                collisionCounter = 0;
            }
            // Advance turn to next in line
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
            // End this NPCs turn and inform gameState
            isMoveTurn = false;
            gameState.setNextTurn(false);
        }
    }
}

