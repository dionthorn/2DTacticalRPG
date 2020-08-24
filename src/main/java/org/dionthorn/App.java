package org.dionthorn;

/**
 * JavaFX App entry point. This is just used for the jlink Image generation as
 * it doesn't like it when the .main() entry class extends Application so we create this shell entry point.
 * When normally developing you can ignore this and directly run Run.java
 */
public class App {

    public static void main(String[] args) { Run.main(args); }

}