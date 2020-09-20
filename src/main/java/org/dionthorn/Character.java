package org.dionthorn;

import javafx.scene.image.Image;

import java.io.File;
import java.util.logging.Level;

/**
 * The Character Class will handle all information regarding animated 'Character's in the game.
 */
public abstract class Character extends PhysicalEntity implements Drawable, Updateable {

    protected final String PATH;
    protected TileSet spriteSheet;
    protected Image currentSprite;
    protected int tileSize;
    protected CharacterClass charClass;
    protected String name;
    protected double maxHP;
    protected double hp;
    protected double attack;
    protected int lastAttackRoll = 0;
    protected double critical;
    protected double defense;
    protected boolean isAlive;
    protected boolean isMoveTurn;
    protected boolean isBattleTurn;
    protected boolean isAttacking;

    /**
     * Default Abstract Character Constructor will generate everything needed for a 'character' in the game
     * @param map this characters assigned map
     * @param spritePath this characters sprite sheet path
     * @param name this characters name
     * @param x this characters x location
     * @param y this characters y location
     * @param charClass this characters class
     */
    public Character(Map map, String spritePath, String name, int x, int y, CharacterClass charClass) {
        super(map, x, y);
        PATH = Run.GAME_DATA_PATH + File.separator + "Art" + File.separator + spritePath;
        tileSize = 32;
        spriteSheet = new TileSet(spritePath, 160); // These sprite are larger than 32*32, however we can cut
        // them into 160*160 and then resize the image to 32*32 during the Player or NPC subclasses .draw() calls
        currentSprite = spriteSheet.getTile(0);
        this.name = name;
        this.hp = charClass.getPerLevelHP();
        this.attack = charClass.getPerLevelAttack();
        this.critical = charClass.getPerLevelCritical();
        this.defense = charClass.getPerLevelDefense();
        this.maxHP = this.hp;
        this.isAlive = true;
        this.charClass = charClass;
    }

    /**
     * Will check if this character will collide with an ally using game state and the x,y of the location to test.
     * Will return false if no ally character is found at x,y otherwise true
     * Will also return true if a tile of type Impassable
     * If an ally isn't found there we will apply fire damage if the tile is of type fire.
     * @param gameState the current game state this character is associated to
     * @param x the x location to check for an ally
     * @param y the y location to check for an ally
     * @return returns true if an ally or impassable tile is occupying x,y and false otherwise.
     */
    protected boolean checkFriendlyCollision(GameState gameState, double x, double y) {
        boolean hit = false;
        if(gameState.getPlayerTeam().contains(this)) {
            for(Entity e: gameState.getPlayerTeam()) {
                int targetX = ((PhysicalEntity) e).getX();
                int targetY = ((PhysicalEntity) e).getY();
                if (targetX == x && targetY == y) {
                    hit = true;
                    break;
                }
            }
        } else if(gameState.getEnemyTeam().contains(this)) {
            for(Entity e: gameState.getEnemyTeam()) {
                int targetX = ((PhysicalEntity) e).getX();
                int targetY = ((PhysicalEntity) e).getY();
                if (targetX == x && targetY == y) {
                    hit = true;
                    break;
                }
            }
        }
        double tempX = x;
        double tempY = y;
        if(tempX < 0) {
            tempX = 0;
        } else if(tempX >= gameState.getCurrentMap().getMapWidth()) {
            tempX = gameState.getCurrentMap().getMapWidth() - 1;
        }
        if(tempY < 0) {
            tempY = 0;
        } else if(tempY >= gameState.getCurrentMap().getMapHeight()) {
            tempY = gameState.getCurrentMap().getMapHeight() - 1;
        }
        if(gameState.getCurrentMap().getTileType((int) tempX, (int) tempY) == MapTile.TileType.IMPASSABLE) {
            hit = true;
        }
        if(gameState.getCurrentMap().getTileType((int) tempX, (int) tempY) == MapTile.TileType.FIRE) {
            (this).setHp(this.getHp()-((int)((this).getMaxHP()/10)));
        }
        return hit;
    }

    /**
     * Will check if this character will collide with an enemy using game state and the x,y of the location to test.
     * Will return null if no character is found at x,y
     * @param gameState the current game state this character is associated to
     * @param x the x location to check for an enemy
     * @param y the y location to check for an enemy
     * @return the Character that was collided with, will return null if x,y is not occupied
     */
    protected Character checkEnemyCollision(GameState gameState, double x, double y) {
        boolean hit = false;
        int targetUID = -1;
        if(gameState.getPlayerTeam().contains(this)) {
            for(Entity e: gameState.getEnemyTeam()) {
                int targetX = ((PhysicalEntity) e).getX();
                int targetY = ((PhysicalEntity) e).getY();
                if(targetX == x && targetY == y) {
                    hit = true;
                    targetUID = e.getUID();
                }
            }
        } else if(gameState.getEnemyTeam().contains(this)) {
            for(Entity e: gameState.getPlayerTeam()) {
                int targetX = ((PhysicalEntity) e).getX();
                int targetY = ((PhysicalEntity) e).getY();
                if(targetX == x && targetY == y) {
                    hit = true;
                    targetUID = e.getUID();
                }
            }
        }
        if(hit) {
            for(Entity e: gameState.getEntities()) {
                if(e.getUID() == targetUID) {
                    return (Character) e;
                }
            }
        }
        return null;
    }

    /**
     * Performs an attack where this character attacks the provided enemy.
     * @param enemy the character that this character will attack
     */
    protected void attack(Character enemy) {
        Dice d100 = new Dice(100);
        int attackRoll = d100.roll();
        lastAttackRoll = attackRoll;
        if(attackRoll <= 70) {
            double reduction = attack;
            Run.programLogger.log(Level.INFO, "[" + getUID() + "] " + getName() + System.lineSeparator() +
                    "Rolled: " + attackRoll
            );
            if(attackRoll <= 30) {
                reduction = critical;
                Run.programLogger.log(Level.INFO, "Critical Hit!");
            }
            reduction -= enemy.getDefense() * 0.10;
            enemy.setHp(enemy.getHp() - reduction);
            Run.programLogger.log(Level.INFO, "Attack damage: " + reduction + System.lineSeparator() +
                    "Enemy HP: " + enemy.getHp()
            );
        } else {
            Run.programLogger.log(Level.INFO, "[" + getUID() + "] " + getName() + System.lineSeparator() +
                    "Missed!" + System.lineSeparator() +
                    "Rolled: " + attackRoll
            );
        }
    }

    /**
     * Performs the needed steps to level up a character
     */
    protected void levelUp() {
        setAttack(attack + charClass.getPerLevelAttack());
        setCritical(critical + charClass.getPerLevelCritical());
        setDefense(defense + charClass.getPerLevelDefense());
        maxHP += charClass.getPerLevelHP();
        hp = maxHP;
        charClass.levelUp();
    }

    // Getters and Setters
    /**
     * Returns the name of the character.
     * @return the name of the character
     */
    protected String getName() { return name; }

    /**
     * Returns the hp attribute of the character.
     * @return the hp attribute of the character
     */
    protected double getHp() { return hp; }

    /**
     * Returns the max hp attribute of the character.
     * @return the max hp attribute of the character
     */
    protected double getMaxHP() { return maxHP; }

    /**
     * Returns the attack attribute of the character.
     * @return the attack attribute of the character
     */
    protected double getAttack() { return attack; }

    /**
     * Returns the critical attribute of the character.
     * @return the critical attribute of the character
     */
    protected double getCritical() { return critical; }

    /**
     * Returns the defense attribute of the character.
     * @return the defense attribute of the character
     */
    protected double getDefense() { return defense; }

    /**
     * Will set the characters hp attribute to the provided newHP. Will set the character as dead if less than 0 or 0.
     * @param newHP set the characters hp attribute to the provided double
     */
    protected void setHp(double newHP) {
        hp = newHP;
        if(hp <= 0) {
            isAlive = false;
            hp = 0;
        } else if(hp > maxHP) {
            hp = maxHP;
        }
    }

    /**
     * Will set the characters attack attribute to the provided newAttack.
     * @param newAttack set the characters attack attribute to the provided double
     */
    protected void setAttack(double newAttack) {
        attack = newAttack;
        if(attack <= 0) {
            attack = 1;
        }
    }

    /**
     * Will set the characters critical attribute to the provided newCritical.
     * @param newCritical set the characters critical attribute to the provided double
     */
    protected void setCritical(double newCritical) {
        critical = newCritical;
        if(critical <= 0) {
            critical = 1;
        }
    }

    /**
     * Will set the characters defense attribute to the provided newDefense.
     * @param newDefense set the characters defense attribute to the provided double
     */
    protected void setDefense(double newDefense) {
        defense = newDefense;
        if(defense <= 0) {
            defense = 1;
        }
    }

    /**
     * Returns the boolean alive flag for this character.
     * @return the boolean alive flag for this character
     */
    protected boolean isAlive() { return isAlive; }

    /**
     * Returns the boolean move turn flag for this character
     * @return the boolean move turn flag for this character
     */
    protected boolean isMoveTurn() { return isMoveTurn; }

    /**
     * Sets the characters move turn flag.
     * @param value boolean for setting the characters move turn flag
     */
    protected void setMoveTurn(boolean value) { isMoveTurn = value; }

    /**
     * Returns the boolean value of the characters battle turn flag.
     * @return the boolean value of the characters battle turn flag
     */
    protected boolean isBattleTurn() { return isBattleTurn; }

    /**
     * Sets the characters battle turn flag.
     * @param value boolean for setting the characters battle turn flag
     */
    protected void setBattleTurn(boolean value) { isBattleTurn = value; }

    /**
     * Returns the image of the associated sprite being used.
     * @return the image of the associated sprite being used
     */
    protected Image getCurrentSprite() { return currentSprite; }

    /**
     * Changes the currentSprite tile for animation.
     * @param tileID the id of the sprite for animation purposes
     */
    protected void setCurrentSprite(int tileID) { currentSprite = spriteSheet.getTile(tileID); }

    /**
     * Returns this characters CharacterClass.
     * @return this characters character class
     */
    protected CharacterClass getCharClass() { return charClass; }

    /**
     * Returns the boolean flag isAttacking.
     * @return boolean value of whether this character is currently attacking.
     */
    protected boolean isAttacking() { return isAttacking; }

    /**
     * Will set the characters isAttacking flag.
     * @param value boolean value to set this characters is attacking flag
     */
    protected void setIsAttacking(boolean value) { isAttacking = value;  }

    /**
     * Will perform sprite changes based on provided time for animation purposes.
     * @param time used to select sprites for animation
     */
    protected void attackAnimation(int time) { setCurrentSprite(charClass.attackAnimationStage(time)); }

    protected int getLastAttackRoll() { return lastAttackRoll; }

}

