package org.dionthorn;

/**
 * Updateable interface is used to differentiate between entities that should be updated during .update() calls
 * by the AnimationTimer in Run.start()
 */
public interface Updateable {
    /**
     * Will update the entity using gameState
     * @param gameState the current gameState used to update the entity
     */
    void update(GameState gameState);
}
