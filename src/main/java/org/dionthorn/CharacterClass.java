package org.dionthorn;

/**
 * Abstract CharacterClass just a default template to make more distinct classes with such as Martial and Magic
 * Has basic information about sprite types and animation sequences
 */
public abstract class CharacterClass {

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

    /**
     * Default abstract CharacterClass Constructor will set the basic attributes others are set in child classes
     */
    public CharacterClass() {
        this.level = 1;
        this.currentXP = 0;
    }

    /**
     * Returns boolean true if a level up has occurred when adding xp to this character class other wise returns false.
     * @param xpGain the amount of xp to add to this character class
     * @return boolean true if a level up has occurred otherwise returns false
     */
    protected boolean addXP(int xpGain) {
        currentXP += xpGain;
        int reqXP = level * 1007;
        if(currentXP >= reqXP) {
            currentXP = 0;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Increments level for this class
     */
    protected void levelUp() { level++; }

    // Getters and Setters below
    /**
     * Returns the tileID of the 'dead' sprite from the sprite sheet.
     * @return the tile id of the 'dead' sprite from the sprite sheet
     */
    protected int getDeadTileID() { return deadTileID; }

    /**
     * Returns the current level of the character class.
     * @return the current level of the character class
     */
    protected int getLevel() { return level; }

    /**
     * Returns the current xp of the character class.
     * @return the current xp of the character class
     */
    protected int getCurrentXP() { return currentXP; }

    /**
     * Returns the hp increase amount during level ups for this class.
     * @return the hp increase amount during level ups for this class
     */
    protected int getPerLevelHP() { return perLevelHP; }

    /**
     * Returns the attack increase amount during level ups for this class.
     * @return the attack increase amount during level ups for this class
     */
    protected int getPerLevelAttack() { return perLevelAttack; }

    /**
     * Returns the critical increase amount during level ups for this class.
     * @return the critical increase amount during level ups for this class
     */
    protected int getPerLevelCritical() { return perLevelCritical; }

    /**
     * Returns the defense increase amount during level ups for this class.
     * @return the defense increase amount during level ups for this class
     */
    protected int getPerLevelDefense() { return perLevelDefense; }

    /**
     * Returns the animation stage of the character class.
     * @param time the value of time that has passed since last call
     * @param duration usually will be called by other method overload and default to 30
     * @return the integer value for the next stage of the animation
     */
    protected int attackAnimationStage(int time, int duration) {
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

    /**
     * Returns the attack animation stage for the current character class
     * @param time the value of time that has passed since last call
     * @return the integer value for the next stage of animation
     */
    protected int attackAnimationStage(int time) { return attackAnimationStage(time, 10); }

    /**
     * Returns the completed cycles flag for this character class
     * @return the integer value for amount of cycles completed so far
     */
    protected int getCompletedCycles() { return completedCycles; }

    /**
     * Will set the completed cycles flag for this character class
     * @param value the integer value to set amount of cycles completed so far
     */
    protected void setCompletedCycles(int value) { completedCycles = value; }

    /**
     * Returns the string of the path to the default sprite for an ally of this class.
     * @return the string of the path to the default sprite for an ally of this class
     */
    protected String getDefaultSpriteAlly() { return defaultSpriteAlly; }

    /**
     * Returns the string of the path to the default sprite for an enemy of this class.
     * @return the string of the path to the default sprite for an enemy of this class
     */
    protected String getDefaultSpriteEnemy() { return defaultSpriteEnemy; }

}

