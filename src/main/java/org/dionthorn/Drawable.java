package org.dionthorn;

import javafx.scene.canvas.GraphicsContext;

public interface Drawable {
    // GraphicsContext is from the canvas, it allows us to draw on the canvas
    void draw(GraphicsContext gc);
}
