package org.dionthorn;

import java.util.Random;

public class Dice {
    // Dice class, you could also just get a 'die' with the Dice(faces) constructor
    private Random rand = new Random();
    private int faces;
    private int amount;

    public Dice(int faces) {
        this.faces = faces;
        this.amount = 1;
    }

    public Dice(int faces, int amount) {
        this.faces = faces;
        this.amount = amount;
    }

    public int roll() {
        // Example: new Dice(6, 3).roll() would return the result of rolling 3 six sided die or between 3-18
        //          new Dice(6).roll() would return the result of rolling a single six sided die or between 1-6
        int rollValue = 0;
        for(int die=0; die<amount; die++) {
            rollValue += 1 + rand.nextInt(faces);
        }
        return rollValue;
    }
}
