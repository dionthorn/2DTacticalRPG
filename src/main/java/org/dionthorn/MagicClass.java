package org.dionthorn;

/**
 * The MagicClass defines the 'Mage' or 'Magic' character class type
 * it will provide the needed default information as well as Animation frames and sprite sheet paths
 */
public class MagicClass extends CharacterClass {

    /**
     * Default Constructor assigns level 1 values and meta information on animation for the class
     */
    public MagicClass() {
        this.deadTileID = 8;
        this.perLevelAttack = 10;
        this.perLevelCritical = 15;
        this.perLevelDefense = 5;
        this.perLevelHP = 20;
        this.attackAnimationSequence = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 1, 0 };
        this.defaultSpriteEnemy = "MagicClassComputer.png";
        this.defaultSpriteAlly = "MagicClassComputerAlly.png";
    }

}

