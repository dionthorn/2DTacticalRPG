package org.dionthorn;

/**
 * The MartialClass defines the 'Fighter' or 'Martial' character class type
 * it will provide the needed default information as well as Animation frames and sprite sheet paths
 */
public class MartialClass extends CharacterClass {

    /**
     * Default Constructor assigns level 1 values and meta information on animation for the class
     */
    public MartialClass() {
        this.deadTileID = 4;
        this.perLevelAttack = 5;
        this.perLevelCritical = 20;
        this.perLevelDefense = 20;
        this.perLevelHP = 30;
        this.attackAnimationSequence = new int[] { 0, 1, 2, 3, 2, 1, 2, 3, 0 };
        this.defaultSpriteEnemy = "MartialClassComputer.png";
        this.defaultSpriteAlly = "MartialClassComputerAlly.png";
    }

}