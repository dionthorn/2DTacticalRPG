package org.dionthorn;

import javafx.scene.image.Image;

public abstract class Item extends Entity {

    protected String name;
    protected int value;
    protected double weight;
    private final Image sprite; // Even in inventory should have a sprite in case we render in an inventory screen.

    public Item(String name, int value, double weight, Image sprite) {
        this.name = name;
        this.value = value;
        this.weight = weight;
        this.sprite = sprite;
    }

    public int getValue() {
        return value;
    }

    public double getWeight() {
        return weight;
    }

    public Image getSprite() {
        return sprite;
    }

    public String getName() {
        return name;
    }

    public void stackItem(Item toStack) {
        if(this.name.equals(toStack.getName())) {
            this.value += toStack.getValue();
            this.weight += toStack.getValue();
        } else {
            System.err.println("These Items Are Not the Stackable!");
        }
    }

}
