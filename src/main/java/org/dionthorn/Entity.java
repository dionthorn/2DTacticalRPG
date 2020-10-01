package org.dionthorn;

/**
 * The most basic 'game object' is literally just a reference ID
 * Intended inheritance tree for Alpha:
 * Entity -> PhysicalEntity  -> Character -> PlayerCharacter
 *                            |            |
 *                            |            -> NonPlayerCharacter
 *                            |
 *                            -> Item      -> Tome
 *                                         |> Weapon
 *                                         -> HP Pot
 *         (Below here maybe Beta Version)
 *         -> NonPhysicalEntity -> TimeBasedEvent (maybe use for spells that take more than one turn?)
 */
public abstract class Entity {

    public static int GEN_COUNT = 0;
    protected final int UID = GEN_COUNT++;

    /**
     * Default Entity Constructor will assign the entity a unique id then increment the GEN_COUNT that's it.
     */
    protected Entity() {
        // UID assignment takes place upon construction as defined by UID declaration at the class level.
        // It is then ++ after assignment, giving us a Unique Identification number for every entity.
    }

    /**
     * Returns the integer Unique ID of the entity, no two entities will have the same UID.
     * @return the integer unique id of the entity
     */
    protected int getUID() { return UID; }

}

