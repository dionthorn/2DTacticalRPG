package org.dionthorn;

public class MagicClass extends CharacterClass {

    public MagicClass() {
        this.deadTileID = 8;
        this.perLevelAttack = 10;
        this.perLevelCritical = 15;
        this.perLevelDefense = 5;
        this.perLevelHP = 25;
        this.attackAnimationSequence = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 1, 0 };
        this.defaultSpriteEnemy = "MagicClassComputer.png";
        this.defaultSpriteAlly = "MagicClassComputerAlly.png";
    }

}

