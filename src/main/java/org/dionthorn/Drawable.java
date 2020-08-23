package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;

/**
 * Drawable interface is used to differentiate between entities that should be drawn during .draw() calls
 * by the AnimationTimer used in Run.start()
 */
public interface Drawable {
    /**
     * Will draw the entity to the Canvas objects GraphicsContext
     * @param gc the graphics context is used to draw onto the canvas object during gameplay
     */
    void draw(GraphicsContext gc);
}
