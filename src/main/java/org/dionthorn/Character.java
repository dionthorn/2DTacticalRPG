package org.dionthorn;

import javafx.scene.image.Image;

// All 'Character' objects can both be Drawn and Updated implementation of those methods get written in child classes
public abstract class Character extends PhysicalEntity implements Drawable, Updateable {
    // Graphical variables
    protected final String PATH;
    protected TileSet spriteSheet;
    protected Image currentSprite;
    protected int tileSize;
    // Game logic variables
    protected CharacterClass charClass; // Used to determine levelUp
    protected String name;
    protected double maxHP; // Stats
    protected double hp;
    protected double attack;
    protected double critical;
    protected double defense;
    protected boolean isAlive; // If the character is alive
    protected boolean isMoveTurn; // If it is the characters turn to move on the map
    protected boolean isBattleTurn; // if it is the characters turn to choose to attack/defend in battle
    protected boolean isAttacking; // used for animation handling

    public Character(Map map, String spritePath, String name, int x, int y, CharacterClass charClass) {
        super(map, x, y); // Call super constructor of PhysicalEntity
        PATH = Run.GAME_DATA_PATH + "/Art/" + spritePath; // relative path to the sprite sheet
        tileSize = 32;
        spriteSheet = new TileSet(spritePath, 160);
        currentSprite = spriteSheet.getTile(0);
        this.name = name;
        this.hp = 50; // Default 50hp can be changed!
        this.attack = charClass.getPerLevelAttack();
        this.critical = charClass.getPerLevelCritical();
        this.defense = charClass.getPerLevelDefense();
        this.maxHP = this.hp;
        this.isAlive = true; // Start alive
        this.charClass = charClass; // Assigned class
    }

    protected boolean checkFriendlyCollision(GameState gameState, double x, double y) {
        // Will return true if a friendly character occupies the location at x,y
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
        // Check for Impassable tile
        if(gameState.getCurrentMap().getTileType((int) x, (int) y) == MapTile.TileType.IMPASSABLE) {
            hit = true;
        }
        // This causes damage when character is standing on fire tiles
        if(gameState.getCurrentMap().getTileType((int) x, (int) y) == MapTile.TileType.FIRE) {
            (this).setHp(this.getHp()-((int)((this).getMaxHP()/10)));
        }
        return hit;
    }

    protected Character checkEnemyCollision(GameState gameState, double x, double y) {
        // Will check if the location at x,y is occupied by an enemy character
        // Will return the enemy entity that was detected or null
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

    protected void attack(Character enemy) {
        // Used for attack logic
        Dice d100 = new Dice(100);
        int attackRoll = d100.roll();
        if(attackRoll <= 70) {
            // 70% chance of hitting
            double reduction = attack; // Can alter reduction to take into account defense stat
            if(Run.DEBUG_OUTPUT) {
                System.out.println("[" + getUID() + "] " + getName());
                System.out.println("Rolled: " + attackRoll);
            }
            if(attackRoll <= 30) {
                // 20% chance of critical hit
                reduction = critical; // Multiply reduction by 1.5 times?
                if(Run.DEBUG_OUTPUT) {
                    System.out.println("Critical!");
                }
            }
            reduction -= enemy.getDefense() * 0.10; // 1/10th of defense stat absorbed damage
            enemy.setHp(enemy.getHp() - reduction); // Deal damage
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

    protected void levelUp() {
        setAttack(attack + charClass.getPerLevelAttack());
        setCritical(critical + charClass.getPerLevelCritical());
        setDefense(defense + charClass.getPerLevelDefense());
        maxHP += charClass.getPerLevelHP(); // will give full hp on a level up
        hp = maxHP;
        charClass.levelUp();
    }

    // Getters and Setters
    protected String getName() { return name; }

    protected double getHp() { return hp; }

    protected double getMaxHP() { return maxHP; }

    protected double getAttack() { return attack; }

    protected double getCritical() { return critical; }

    protected double getDefense() { return defense; }

    protected void setHp(double newHP) {
        hp = newHP;
        if(hp <= 0) {
            // If the new HP is less than or equal to zero set character as dead
            isAlive = false;
            hp = 0;
        } else if(hp > maxHP) {
            // Cannot go over maximum hp
            hp = maxHP;
        }
    }

    protected void setAttack(double newAttack) {
        attack = newAttack;
        if(attack <= 0) {
            attack = 1;
        }
    }

    protected void setCritical(double newCritical) {
        critical = newCritical;
        if(critical <= 0) {
            critical = 1;
        }
    }

    protected void setDefense(double newDefense) {
        defense = newDefense;
        if(defense <= 0) {
            defense = 1;
        }
    }

    protected boolean isAlive() { return isAlive; }

    protected boolean isMoveTurn() { return isMoveTurn; }

    protected void setMoveTurn(boolean value) { isMoveTurn = value; }

    protected boolean isBattleTurn() { return isBattleTurn; }

    protected void setBattleTurn(boolean value) { isBattleTurn = value; }

    protected Image getCurrentSprite() { return currentSprite; }

    protected void setCurrentSprite(int tileID) {
        /* ability to change sprite tile for simple 'animation'*/
        currentSprite = spriteSheet.getTile(tileID);
    }

    protected CharacterClass getCharClass() { return charClass; }

    protected boolean IsAttacking() { return isAttacking; }

    protected void setIsAttacking(boolean value) { isAttacking = value;  }

    protected void attackAnimation(int time) { setCurrentSprite(charClass.attackAnimationStage(time)); }
}

