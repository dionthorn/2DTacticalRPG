package org.dionthorn;

public abstract class Item {

    protected int value;
    protected double weight;

    public Item(int value, double weight) {
        this.value = value;
        this.weight = weight;
    }

    public int getValue() {
        return value;
    }

    public double getWeight() {
        return weight;
    }

}
