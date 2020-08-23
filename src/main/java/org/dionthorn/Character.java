package org.dionthorn;

import javafx.scene.image.Image;

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
        PATH = Run.GAME_DATA_PATH + "/Art/" + spritePath;
        tileSize = 32;
        spriteSheet = new TileSet(spritePath, 160);
        currentSprite = spriteSheet.getTile(0);
        this.name = name;
        this.hp = 50;
        this.attack = charClass.getPerLevelAttack();
        this.critical = charClass.getPerLevelCritical();
        this.defense = charClass.getPerLevelDefense();
        this.maxHP = this.hp;
        this.isAlive = true;
        this.charClass = charClass;
    }

    /**
     * Will check if this character will collide with an ally using gamestate and the x,y of the location to test.
     * Will return false if no ally character is found at x,y otherwise true
     * Will also return true if a tile of type Impassable
     * If an ally isn't found there we will apply fire damage if the tile is of type fire.
     * @param gameState the current gamestate this character is associated to
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
                if(targetX == x && targetY == y) {
                    hit = true;
                }
            }
        } else if(gameState.getEnemyTeam().contains(this)) {
            for(Entity e: gameState.getEnemyTeam()) {
                int targetX = ((PhysicalEntity) e).getX();
                int targetY = ((PhysicalEntity) e).getY();
                if(targetX == x && targetY == y) {
                    hit = true;
                }
            }
        }
        if(gameState.getCurrentMap().getTileType((int) x, (int) y) == MapTile.TileType.IMPASSABLE) {
            hit = true;
        }
        if(gameState.getCurrentMap().getTileType((int) x, (int) y) == MapTile.TileType.FIRE) {
            (this).setHp(this.getHp()-((int)((this).getMaxHP()/10)));
        }
        return hit;
    }

    /**
     * Will check if this character will collide with an enemy using gamestate and the x,y of the location to test.
     * Will return null if no character is found at x,y
     * @param gameState the current gamestate this character is associated to
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
            return (Character) gameState.getEntities().get(targetUID);
        } else {
            return null;
        }
    }

    /**
     * Performs an attack where this character attacks the provided enemy.
     * @param enemy the character that this character will attack
     */
    protected void attack(Character enemy) {
        Dice d100 = new Dice(100);
        int attackRoll = d100.roll();
        if(attackRoll <= 70) {
            double reduction = attack;
            if(Run.DEBUG_OUTPUT) {
                System.out.println("[" + getUID() + "] " + getName());
                System.out.println("Rolled: " + attackRoll);
            }
            if(attackRoll <= 30) {
                reduction = critical;
                if(Run.DEBUG_OUTPUT) {
                    System.out.println("Critical!");
                }
            }
            reduction -= enemy.getDefense() * 0.10;
            enemy.setHp(enemy.getHp() - reduction);
            if(Run.DEBUG_OUTPUT) {
                System.out.println("Attack damage: " + reduction);
                System.out.println("Enemy HP: " + enemy.getHp());
            }
        } else {
            if(Run.DEBUG_OUTPUT) {
                System.out.println("[" + getUID() + "] " + getName());
                System.out.println("Missed!");
                System.out.println("Rolled: " + attackRoll);
            }
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
    protected void setCurrentSprite(int tileID) {
        currentSprite = spriteSheet.getTile(tileID);
    }

    /**
     * Returns this characters CharacterClass.
     * @return this characters character class
     */
    protected CharacterClass getCharClass() { return charClass; }

    /**
     * Returns the boolean flag isAttacking.
     * @return boolean value of whether this character is currently attacking.
     */
    protected boolean IsAttacking() { return isAttacking; }

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

}

