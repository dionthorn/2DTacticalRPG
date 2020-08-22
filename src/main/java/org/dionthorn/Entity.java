package org.dionthorn;

public abstract class Entity {
    // The most basic 'game object' is literally just a reference ID
    // Intended inheritance tree for Alpha:
    // Entity -> PhysicalEntity  -> Character -> PlayerCharacter
    //                           |            |
    //                           |            -> NonPlayerCharacter
    //                           |
    //                           -> Item      -> Tome
    //                                        |> Weapon
    //                                        -> HP Pot
    //        (Below here maybe Beta Version)
    //        -> NonPhysicalEntity -> TimeBasedEvent (maybe use for spells that take more than one turn?)
    public static int GEN_COUNT = 0; // Used to give a unique ID to each game object
    protected final int UID; // Unique identifier of a game object

    protected Entity() {
        UID = GEN_COUNT;
        GEN_COUNT++;
    }

    protected int getUID() { return UID; }
}

