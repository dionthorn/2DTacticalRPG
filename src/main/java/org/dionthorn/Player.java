package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;

public class Player extends Character {

    public Player(Map map, String spritePath, String name, int x, int y, CharacterClass charClass) {
        super(map, spritePath, name, x, y, charClass);
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Will be called by the JavaFX AnimationTimer each frame!
        // the gc can be used to draw onto the canvas
        gc.drawImage(getCurrentSprite(), getX() * tileSize, getY() * tileSize, tileSize, tileSize);
    }

    @Override
    public void update(GameState gameState) {
        // Will be called just before a draw() call, can be used to update
        // in between frames if needed (maybe not need for player since keyboard input is turn based)
    }
}
