# 2DTacticalRPG

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/e02ce09553a5481092fd7ed8398a0593)](https://app.codacy.com/manual/dionthorn/2DTacticalRPG?utm_source=github.com&utm_medium=referral&utm_content=dionthorn/2DTacticalRPG&utm_campaign=Badge_Grade_Dashboard)

Compiled with OpenJDK 13 and OpenJFX 13.
I use OpenJDK from https://adoptopenjdk.net/ 13 but 11/14/15 should compile fine as far as I'm aware.
I use OpenJFX from https://openjfx.io/ same applies as above.
All github listed dependencies are related to maven plugins used to generate the jlink image and handle compiling etc.

I've added a Wiki to this Github Repo if you have any questions on how the system works please see https://github.com/dionthorn/2DTacticalRPG/wiki

To test on windows (look into running jlink images with linux if your on linux) go to https://github.com/dionthorn/2DTacticalRPG/releases
Then choose the latest release and under 'assets' get the TacticalRPG{version}.zip file, then extract where you wish, go into the folder and into /bin then run App.bat

If you get the latest version of IntellijIDEA community edition for free and create a new Maven project and setup the openjfx archetype: https://openjfx.io/openjfx-docs/#IDE-Intellij
Then you can go to the project folder in your /IdeaProjects/yourProject folder and copy the pom.xml, src, and MavenTactical.iml (rename this to yourProject.iml) It appears it will only compile using the maven compile then maven javafx:run goals as that points it to the target/classes folder for loading images behaviour.

Controls: 
	Arrow Keys to move character, move into an enemy to start a battle.
	Space bar to advance player turn or NPC turn, you will be prompted in the bottom part of the game screen.
	For DevMenu -> use Tilde when a map is being rendered (not in the Main_Menu/Game_Over states)
DevMenu: 
	When in DevMenu you can check the EditMode box to 'pause' the game function and allow you to click a tile on the tileset, then draw onto the map with the mouse. A right click will delete the current tile. You can click and drag an area to paint the whole square/rectangle area.
	You can change the MapID with the '+' and '-' buttons if you go beyond the amount of maps loaded in memory it will auto randomly generate a map based on the currently loaded tilesets. Once you've created the map you can then 'save' the map you will need to provide .meta data to compliment the .dat file for the map.

Here is a short youtube video of the game in action: https://www.youtube.com/watch?v=TNlHSJetf1Q

![Alt text](/ExampleScreenShots/gameExample.PNG?raw=true "Game Example")

![Alt text](/ExampleScreenShots/devMenuExample.PNG?raw=true "Dev Menu")

![Alt text](/ExampleScreenShots/graphicalLevelSelection.PNG?raw=true "Level Selection")

v0.2.1 Notes: a complete restructuring of the code base using Intellij and Maven to produce jlink images rather than .jar files. Several other code maintence events have happend as well as the addition of the Level Selection screen.

v0.2.0 Notes: added more functionality to the DevMenu including the ability to manually change gameState. 
	Added but not implemented a 'level selection' state to allow graphical selection of levels.
	When loading a new map the engine will now load .meta data to generate NPCs both allies and enemies
