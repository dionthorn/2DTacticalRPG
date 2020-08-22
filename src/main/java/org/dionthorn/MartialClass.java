package org.dionthorn;

public class MartialClass extends CharacterClass {

    public MartialClass() {
        this.deadTileID = 4;
        this.perLevelAttack = 5;
        this.perLevelCritical = 20;
        this.perLevelDefense = 20;
        this.perLevelHP = 25;
        this.attackAnimationSequence = new int[] { 0, 1, 2, 3, 2, 1, 2, 3, 0 };
        this.defaultSpriteEnemy = "MartialClassComputer.png";
        this.defaultSpriteAlly = "MartialClassComputerAlly.png";
    }

}

