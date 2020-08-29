package org.dionthorn;

import java.util.Random;

/**
 * The Dice class will be used for game related random 'rolls'. Easily the most portable class in the game.
 */
public class Dice {

    private final Random rand = new Random();
    private final int faces;
    private final int amount;

    /**
     * Default Dice Constructor gives 1 'die' with faces being the size of the die values
     * @param faces the count of the sides of the die
     */
    public Dice(int faces) {
        this.faces = faces;
        this.amount = 1;
    }

    /**
     * Dice Constructor gives a number of dice defined by amount where faces is the amount of sides on the dice
     * @param faces the count of the sides of the die
     * @param amount how many die in the dice set
     */
    public Dice(int faces, int amount) {
        this.faces = faces;
        this.amount = amount;
    }

    /**
     * Returns the integer value of the sum of the dice roll. Example:
     *  new Dice(6, 3).roll() would return the result of rolling 3 six sided die or between 3-18
     *  new Dice(6).roll() would return the result of rolling a single six sided die or between 1-6
     * @return the integer value of the sum of the dice roll
     */
    public int roll() {
        int rollValue = 0;
        for(int die=0; die<amount; die++) {
            rollValue += 1 + rand.nextInt(faces);
        }
        return rollValue;
    }

}
