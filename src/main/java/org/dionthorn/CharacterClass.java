package org.dionthorn;

public abstract class CharacterClass {
    // Manages level/xp
    protected int level;
    protected int currentXP;
    protected int perLevelHP;
    protected int perLevelAttack;
    protected int perLevelCritical;
    protected int perLevelDefense;
    protected int deadTileID;
    protected int[] attackAnimationSequence;
    protected int completedCycles = 0;
    protected String defaultSpriteAlly;
    protected String defaultSpriteEnemy;

    //establish characters
    public CharacterClass() {
        this.level = 1;
        this.currentXP = 0;
    }

    protected boolean addXP(int xpGain) {
        currentXP += xpGain;
        // Level * XPScale = Needed XP
        // 1 * 1007 = 1007, 2 * 1007 = 2014 ...
        int reqXP = level * 1007;
        if(currentXP >= reqXP) {
            currentXP = 0;
            // We call level up only if this returns true.
            return true;
        } else {
            return false;
        }
    }

    protected void levelUp() { level++; }

    // Getters and Setters below
    protected int getDeadTileID() { return deadTileID; }

    protected int getLevel() { return level; }

    protected int getCurrentXP() { return currentXP; }

    protected int getPerLevelHP() { return perLevelHP; }

    protected int getPerLevelAttack() { return perLevelAttack; }

    protected int getPerLevelCritical() { return perLevelCritical; }

    protected int getPerLevelDefense() { return perLevelDefense; }

    protected int attackAnimationStage(int time, int duration) {
        // Hard coded 30 / either an 8 or 9 depending on CharClass
        int division = duration/attackAnimationSequence.length;
        if(division >= attackAnimationSequence.length) {
            division = attackAnimationSequence.length - 1;
        }
        int spriteChange = time/division;
        if(spriteChange >= attackAnimationSequence.length) {
            spriteChange = 0;
        }
        if(spriteChange == attackAnimationSequence.length - 1) {
            completedCycles++;
        }
        return attackAnimationSequence[spriteChange];
    }

    protected int attackAnimationStage(int time) { return attackAnimationStage(time, 30); }

    protected int getCompletedCycles() { return completedCycles; }

    protected void setCompletedCycles(int value) { completedCycles = value; }

    public String getDefaultSpriteAlly() {
        return defaultSpriteAlly;
    }

    public String getDefaultSpriteEnemy() {
        return defaultSpriteEnemy;
    }

}

