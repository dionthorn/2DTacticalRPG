package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;

public class Player extends Character {

    /**
     * Default Constructor used to create the Player entity.
     * @param map the Map that the Player object is connected to
     * @param spritePath the path to the sprite for this Player
     * @param name the name of the Player
     * @param x the x location of the Player
     * @param y the y location of the Player
     * @param charClass the Class type of the Player
     */
    public Player(Map map, String spritePath, String name, int x, int y, CharacterClass charClass) {
        super(map, spritePath, name, x, y, charClass);
    }

    /**
     * Used to draw the Player.
     * @param gc the graphics context is used to draw onto the canvas object during gameplay
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.drawImage(getCurrentSprite(), getX() * tileSize, getY() * tileSize, tileSize, tileSize);
    }

    /**
     * Used to update the Player.
     * @param gameState the current gameState used to update the entity
     */
    @Override
    public void update(GameState gameState) {
        // Does nothing for now
    }
}
