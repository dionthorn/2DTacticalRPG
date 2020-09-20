package org.dionthorn;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;

/**
 * DevMenu Class will be a secondary window that can be used for development related functions
 * Such as draw maps, editing maps, etc.
 */
public class DevMenu extends Stage {

    private final Run app;
    public int SELECTED_TILE_ID = 0;
    public int SELECTED_TILE_SET_ID = 0;
    public int SELECTED_MAP_ID = 0;
    public boolean EDIT_MODE;
    private final GridPane devMenu;
    private final Text devTileID;
    private final Text devTileSetID;
    private final Text devMapID;
    private final Text devMapPath;
    private ScrollPane charInfoPane;
    private final CheckBox isFire;
    private final CheckBox isImpassable;
    private ImageView tileSetView;
    private final ComboBox<String> mapList = new ComboBox<>();
    private final ComboBox<String> stateList = new ComboBox<>();
    private ArrayList<Text> charInfo;
    private ArrayList<Text> memInfo;
    private final Font arial = new Font("Arial", 10);
    private String[] folders;
    private String shortPath;

    /**
     * Default DevMenu Constructor will generate the devMenu
     * @param app the main game application as a reference, so we can draw and such on the map.
     */
    public DevMenu(Run app) {
        // assign app and setup window basics
        this.app = app;
        this.setTitle(String.format("DEVMENU %s", Run.PROGRAM_VERSION));
        this.setX(0);
        this.setY(0);
        // assign Group, Scene, and BorderPane roots nodes
        Group devRoot = new Group();
        Scene devRootScene = new Scene(devRoot, Run.SCREEN_WIDTH, Run.SCREEN_HEIGHT);
        BorderPane devMainUI = new BorderPane();
        devRoot.getChildren().add(devMainUI);
        // create the devMenu GridPane object, this will be the main UI pane for menu
        devMenu = new GridPane();
        // devMenu.setGridLinesVisible(true); // testing purposes
        devMenu.setHgap(10); // assign a 10 pixel gap between nodes
        devMenu.setVgap(10);
        devMainUI.setCenter(devMenu); // this is the primary node so center it
        devMenu.setOnMouseClicked(this::devMenuClicked); // handy activation of devMenuClicked method,
        // event -> devMenuClicked() lambda will also work here
        // create the TileID: text object that shows the currently selected tileID

        // Row 0
        devTileSetID = new Text(String.format("TileSet ID: %s", SELECTED_TILE_SET_ID));
        devTileSetID.setFont(new Font("Arial", 16));
        GridPane.setConstraints(devTileSetID, 0, 0);
        Button increaseTileSetID = new Button("+");
        GridPane.setConstraints(increaseTileSetID, 1, 0);
        increaseTileSetID.setOnAction(event -> tileSetMaxCheck());
        Button decreaseTileSetID = new Button("-");
        GridPane.setConstraints(decreaseTileSetID, 2, 0);
        decreaseTileSetID.setOnAction(event -> tileSetMinCheck());

        // Row 1

        devMapID = new Text(String.format("Map ID: %s", SELECTED_MAP_ID));
        devMapID.setFont(new Font("Arial", 16));
        GridPane.setConstraints(devMapID, 0, 1);
        Button increaseMapID = new Button("+");
        GridPane.setConstraints(increaseMapID, 1, 1);
        increaseMapID.setOnAction(event -> mapIDMaxCheck());
        Button decreaseMapID = new Button("-");
        GridPane.setConstraints(decreaseMapID, 2, 1);
        decreaseMapID.setOnAction(event -> mapIDMinCheck());

        // Row 2

        devMapPath = new Text("null");
        devMapPath.setFont(new Font("Arial", 10));
        GridPane.setConstraints(devMapPath, 0, 2);
        Button saveButton = new Button("Save Map");
        GridPane.setConstraints(saveButton, 1, 2, 2, 1);
        saveButton.setOnAction(event -> app.getGameState().getCurrentMap().saveData());

        // Row 3

        GridPane.setConstraints(mapList, 0, 3);
        Button loadMap = new Button("Load Map");
        GridPane.setConstraints(loadMap, 1, 3, 2, 1);
        loadMap.setOnAction(event -> {
            for(Map toLoad: app.getGameState().getMaps()) {
                String fullPath = Run.GAME_DATA_PATH + File.separator + "Maps" + File.separator + mapList.getSelectionModel().getSelectedItem();
                if(fullPath.equals(toLoad.getPATH())) {
                    app.getGameState().setCurrentMap(toLoad);
                    Player temp = app.getGameState().getPlayerEntity();
                    temp.setCurrentMap(app.getGameState().getCurrentMap(), 0, 0);
                }
            }
        });

        // Row 4

        Button devUpdate = new Button("Update");
        GridPane.setConstraints(devUpdate, 0, 4);
        devUpdate.setOnAction(event -> app.update());
        Button devLevelUp = new Button("Level Up");
        GridPane.setConstraints(devLevelUp, 1, 4, 2, 1);
        devLevelUp.setOnAction(event -> {
            if (app.getLastSelectChar() != -1) {
                ((Character) app.getGameState().getEntities().get(app.getLastSelectChar())).levelUp();
            }
        });
        Button devMakeIcon = new Button("Make Icon");
        GridPane.setConstraints(devMakeIcon, 3, 4, 2, 1);
        devMakeIcon.setOnAction(event -> makeIcon());

        // Row 5

        Button memData = new Button("Update resource usage data");
        GridPane.setConstraints(memData, 0, 5);
        memData.setOnAction(event -> {
            if(memInfo != null) {
                devMenu.getChildren().remove(memInfo.get(0));
                devMenu.getChildren().remove(memInfo.get(1));
            }
            memInfo = new ArrayList<>();
            long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long usedMem = maxMem - freeMem;
            String total = String.format("MAX:%d Mb TOT:%d Mb", maxMem, totalMem);
            String run = String.format("FRE:%d Mb USE:%d Mb", freeMem, usedMem);
            memInfo.add(new Text(total));
            memInfo.add(new Text(run));
            GridPane.setConstraints(memInfo.get(0), 1, 5, 4, 1);
            devMenu.getChildren().add(memInfo.get(0));
            GridPane.setConstraints(memInfo.get(1), 5, 5, 4, 1);
            devMenu.getChildren().add(memInfo.get(1));
        });

        // Row 6-7

        Button entityInfo = new Button("Update Entity Info");
        GridPane.setConstraints(entityInfo, 0, 6);
        entityInfo.setOnAction(event -> {
            if(charInfo != null) {
                for(Text t: charInfo) {
                    devMenu.getChildren().remove(t);
                }
            }
            if(charInfoPane != null) {
                charInfoPane.setContent(null);
            }
            charInfo = new ArrayList<>();
            for(Entity e: app.getGameState().getEntities()) {
                if(e instanceof Character) {
                    Character temp = (Character) e;
                    Text tempText = new Text(String.format(
                            "[%d] %s (%d, %d) HP:%.2f/%.2f Atk/Def:%.1f/%.1f Critical: %.2f",
                            e.getUID(), temp.getName(),
                            temp.getX(), temp.getY(),
                            temp.getHp(), temp.getMaxHP(),
                            temp.getAttack(), temp.getDefense(),
                            temp.getCritical()
                    ));
                    tempText.setFont(arial);
                    charInfo.add(tempText);
                }
            }
            VBox infoList = new VBox();
            for(Text t: charInfo) {
                infoList.getChildren().add(t);
            }
            charInfoPane = new ScrollPane();
            charInfoPane.setContent(infoList);
            GridPane.setConstraints(charInfoPane, 0, 7, 3, 1);
            devMenu.getChildren().add(charInfoPane);
        });

        // Row 8

        for(GameState.STATE state: GameState.STATE.values()) {
            stateList.getItems().add(state.name());
        }
        GridPane.setConstraints(stateList, 0, 8);
        Button updateState = new Button("!Update State! CAN BREAK ENGINE");
        GridPane.setConstraints(updateState, 1, 8, 5, 1);
        updateState.setOnAction(event -> {
            if(stateList.getSelectionModel().getSelectedItem() != null &&
                    !stateList.getSelectionModel().getSelectedItem().equals("")
            ) {
                String temp = stateList.getSelectionModel().getSelectedItem();
                for(GameState.STATE state: GameState.STATE.values()) {
                    if(state.name().equals(temp)) {
                        app.getGameState().setState(state);
                    }
                }
            }
        });

        // Row 9

        devTileID = new Text(String.format("TileID: %s", SELECTED_TILE_ID));
        devTileID.setFont(new Font("Arial", 16));
        GridPane.setConstraints(devTileID, 0, 9);
        // create the tileID + button
        Button increaseTileID = new Button("+");
        GridPane.setConstraints(increaseTileID, 1, 9);
        increaseTileID.setOnAction(event -> tileMaxCheck());
        // create the tileID - button
        Button decreaseTileID = new Button("-");
        GridPane.setConstraints(decreaseTileID, 2, 9);
        decreaseTileID.setOnAction(event -> tileMinCheck());
        // Column Index 3 is the Selected Tile image
        // create the Edit Mode check box, this is used to paint the map during runtime for graphicalf map creation.
        CheckBox editMode = new CheckBox("Edit Mode");
        editMode.setSelected(false);
        EDIT_MODE = false;
        GridPane.setConstraints(editMode, 4, 9);
        editMode.setOnAction(event -> EDIT_MODE = !EDIT_MODE);
        // create the isFire check box, this lets you know if the in-memory value of the selected tileID is of type FIRE
        isFire = new CheckBox("isFire");
        GridPane.setConstraints(isFire, 5, 9);
        isFire.setOnAction(event -> setFire());
        // create the isImpassable check box, does the same as above but with IMPASSABLE type
        isImpassable = new CheckBox("isImpassable");
        GridPane.setConstraints(isImpassable, 6, 9);
        isImpassable.setOnAction(event -> setImpassable());
        tileMetaCheck();

        // Row 10+ are the TileSet Image

        // Assign all components

        devMenu.getChildren().addAll(devTileID, increaseTileID, decreaseTileID, editMode,
                isFire, isImpassable, devUpdate, devLevelUp, devMakeIcon,
                devTileSetID, increaseTileSetID, decreaseTileSetID,
                devMapID, increaseMapID, decreaseMapID,
                devMapPath, saveButton,
                mapList, loadMap,
                memData, entityInfo,
                stateList, updateState);
        this.setX(0D);
        this.setScene(devRootScene);
        this.show();

    }

    /**
     * Mouse interaction handling for dev menu
     * @param event the mouse event passed in by javafx
     */
    private void devMenuClicked(MouseEvent event) {
        Node clickedNode = event.getPickResult().getIntersectedNode();
        if(clickedNode.equals(tileSetView)) {
            double heightDiff = tileSetView.getBoundsInParent().getMinY();
            Bounds bounds = clickedNode.getBoundsInParent();
            double x = event.getX();
            double y = event.getY();
            ImageView view = tileSetView;
            double xScale = bounds.getWidth() / view.getImage().getWidth();
            double yScale = bounds.getHeight() / view.getImage().getHeight();
            x /= xScale;
            y /= yScale;
            int xCord = (int) x;
            int yCord = (int) y;
            yCord -= heightDiff;
            // Run.programLogger.log(Level.INFO, String.format("Ix=%d, Iy=%d", xCord, yCord));
            int tileX = (xCord / app.getGameState().getCurrentMap().getTileSize());
            int tileY = (yCord / app.getGameState().getCurrentMap().getTileSize());
            // Run.programLogger.log(Level.INFO, String.format("Tx=%d, Ty=%d", tileX, tileY));
            int maxX = (int) view.getImage().getWidth() / app.getGameState().getCurrentMap().getTileSize();
            if(tileY > 0) {
                tileY = tileY * maxX;
            }
            int tileIDToChange = tileX + tileY;
            WritableImage toCheck = new WritableImage(app.getGameState().getCurrentMap().getTileSize(),
                    app.getGameState().getCurrentMap().getTileSize()
            );
            PixelReader pr = tileSetView.getImage().getPixelReader();
            int yOffset = (tileY / maxX) * app.getGameState().getCurrentMap().getTileSize();
            int xOffset = tileX * app.getGameState().getCurrentMap().getTileSize();
            for(int yR=0; yR<app.getGameState().getCurrentMap().getTileSize(); yR++) {
                for(int xR=0; xR<app.getGameState().getCurrentMap().getTileSize(); xR++) {
                    toCheck.getPixelWriter().setColor(xR, yR, pr.getColor(xR+xOffset, yR+yOffset));
                }
            }
            if(TileSet.areImagesSame(
                    app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getBlank(), toCheck)
            ) {
                SELECTED_TILE_ID = app.getGameState().getCurrentMap().getTileSet(
                        SELECTED_TILE_SET_ID).getTotalTiles() - 1;
            } else {
                int blankOffset = 0;
                int[] removedIDs = app.getGameState().getCurrentMap().getTileSet(
                        SELECTED_TILE_SET_ID).getRemovedTileID();
                for(int id: removedIDs) {
                    if(tileIDToChange >= id) {
                        blankOffset++;
                    }
                }
                SELECTED_TILE_ID = tileIDToChange - blankOffset;
            }
            devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
        }
        tileMetaCheck();
    }

    /**
     * checks current tile meta data to ensure the check boxes are correct when you click on one.
     */
    public void tileMetaCheck() {
        isFire.setSelected(false);
        isImpassable.setSelected(false);
        for(int tileID: app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getMetaFire()) {
            if(tileID == SELECTED_TILE_ID) {
                isFire.setSelected(true);
            }
        }
        for(int tileID: app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getMetaImpassable()) {
            if(tileID == SELECTED_TILE_ID) {
                isImpassable.setSelected(true);
            }
        }
    }

    /**
     * tag the selected tile id as a fire tile in its meta data
     * this allows quick tileset meta data generation with a graphical ui.
     */
    public void setFire() {
        if(!isFire.isSelected()) {
            app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).setMetaFireID(SELECTED_TILE_ID, true);
            isFire.setSelected(false);
        } else {
            app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).setMetaFireID(SELECTED_TILE_ID, false);
            isFire.setSelected(true);
        }
        tileMetaCheck();
    }

    /**
     * tag the selected tile id as an impassable tile in its meta data
     * this allows quick tileset meta data generation with a graphical ui
     */
    public void setImpassable() {
        app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).setMetaImpassableID(SELECTED_TILE_ID, !isImpassable.isSelected());
        tileMetaCheck();
    }

    /**
     * Will safely increase the selected tile id flag
     */
    private void tileMaxCheck() {
        if(SELECTED_TILE_ID + 1 >= app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getTotalTiles()
        ) {
            Run.programLogger.log(Level.INFO, "TileID cannot exceed TileSet maximum");
        } else {
            SELECTED_TILE_ID++;
            devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
        }
        Run.programLogger.log(Level.INFO, String.format("SELECTED_TILE_ID: %d", SELECTED_TILE_ID));
    }

    /**
     * Will safely decrease the selected tile id flag
     */
    private void tileMinCheck() {
        if(SELECTED_TILE_ID - 1 < 0) {
            Run.programLogger.log(Level.INFO, "TileID cannot exceed map minimum");
        } else {
            SELECTED_TILE_ID--;
            devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
        }
        Run.programLogger.log(Level.INFO, String.format("SELECTED_TILE_ID: %d", SELECTED_TILE_ID));
    }

    /**
     * Will safely increase the selected tile set id flag
     */
    private void tileSetMaxCheck() {
        if(SELECTED_TILE_SET_ID + 1 >= app.getGameState().getCurrentMap().getTileSets().length) {
            Run.programLogger.log(Level.INFO, "TileSetID cannot exceed map maximum");
        } else {
            SELECTED_TILE_SET_ID++;
            if(SELECTED_TILE_ID >= app.getGameState().getCurrentMap().getTileSet(
                    SELECTED_TILE_SET_ID).getTotalTiles()
            ) {
                SELECTED_TILE_ID = app.getGameState().getCurrentMap().getTileSet(
                        SELECTED_TILE_SET_ID).getTotalTiles() - 1;
                devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
            }
            devTileSetID.setText(String.format("TileSet ID: %s", SELECTED_TILE_SET_ID));
        }
    }

    /**
     * Will safely decrease the selected tile set id flag
     */
    private void tileSetMinCheck() {
        if(SELECTED_TILE_SET_ID - 1 < 0) {
            Run.programLogger.log(Level.INFO, "TileSetID cannot exceed map minimum");
        } else {
            SELECTED_TILE_SET_ID--;
            if(SELECTED_TILE_ID >= app.getGameState().getCurrentMap().getTileSet(
                    SELECTED_TILE_SET_ID).getTotalTiles()
            ) {
                SELECTED_TILE_ID = app.getGameState().getCurrentMap().getTileSet(
                        SELECTED_TILE_SET_ID).getTotalTiles() - 1;
                devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
            }
            devTileSetID.setText(String.format("TileSet ID: %s", SELECTED_TILE_SET_ID));
        }
    }

    /**
     * Will safely increase the selected map id flag
     * if you go beyond the map id of the current count of loaded maps it will auto generate a new
     * randomly generated map based off the currently loaded tilesets
     */
    private void mapIDMaxCheck() {
        try {
            app.getGameState().setCurrentMap(app.getGameState().getMaps().get(SELECTED_MAP_ID + 1));
            SELECTED_MAP_ID++;
            devMapID.setText(String.format("Map ID: %s", SELECTED_MAP_ID));
            folders = app.getGameState().getCurrentMap().getPATH().split("\\\\");
            shortPath = folders[folders.length - 1];
            devMapPath.setText(String.format("File Name: %s", shortPath));
            tileMetaCheck();
        } catch (Exception e) {
            Run.programLogger.log(Level.INFO,
                    String.format("Map %d Not Available Creating Random New Map", SELECTED_MAP_ID + 1)
            );
            app.getGameState().getMaps().add(new Map(app.getGameState().getCurrentMap().getTileSetPaths()));
            app.getGameState().setCurrentMap(app.getGameState().getMaps().get(SELECTED_MAP_ID + 1));
            SELECTED_MAP_ID++;
            devMapID.setText(String.format("Map ID: %s", SELECTED_MAP_ID));
            folders = app.getGameState().getCurrentMap().getPATH().split("\\\\");
            shortPath = folders[folders.length - 1];
            devMapPath.setText(String.format("File Name: %s", shortPath));
            mapList.getItems().add(shortPath);
            mapList.getSelectionModel().select(SELECTED_MAP_ID);
            tileMetaCheck();
        }
    }

    /**
     * Will safely decrease the selected map id flag
     */
    private void mapIDMinCheck() {
        if(SELECTED_MAP_ID - 1 < 0) {
            Run.programLogger.log(Level.INFO, "Map ID cannot be less than 0");
        } else {
            try {
                app.getGameState().setCurrentMap(app.getGameState().getMaps().get(SELECTED_MAP_ID - 1));
                SELECTED_MAP_ID--;
                devMapID.setText(String.format("Map ID: %s", SELECTED_MAP_ID));
                folders = app.getGameState().getCurrentMap().getPATH().split("\\\\");
                shortPath = folders[folders.length - 1];
                devMapPath.setText(String.format("File Name: %s", shortPath));
                tileMetaCheck();
            } catch (Exception e) {
                Run.programLogger.log(Level.INFO, String.format("Map %d Not Available", SELECTED_MAP_ID - 1));
            }
        }
    }

    /**
     * Will generate a .png of a 200x200 pixel 'icon' of the map for use on the level_selection screen.
     * if no icon is found then it will just render the name of the .dat file
     * will first check to see if an icon exists, if it does you must rename it from the .dat file name
     * icon names will always be {mapName}_Icon.png located in the /GameData/Art folder
     */
    public void makeIcon() {
        String[] mapsFolder = app.getGameState().getCurrentMap().getPATH().split("\\\\");
        String targetName = mapsFolder[mapsFolder.length - 1].split("\\.")[0] + "_Icon.png";
        String[] artFiles = FileOpUtils.getFileNamesFromDirectory(Run.GAME_ART_PATH);
        boolean found = false;
        for(String name: artFiles) {
            if(name != null && name.equals(targetName)) {
                found = true;
                break;
            }
        }
        if(found) {
            Run.programLogger.log(Level.INFO, "Icon for this map already exists.");
        } else {
            Canvas toSaveCanvas = app.getCanvas();
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            Image toSaveImage = toSaveCanvas.snapshot(params, null);
            ImageView temp = new ImageView(toSaveImage);
            temp.setFitWidth(200);
            temp.setFitHeight(200);
            toSaveImage = temp.snapshot(null, null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(toSaveImage, null), "png",
                        new File(Run.GAME_ART_PATH + targetName)
                );
                Run.programLogger.log(Level.INFO,
                        "The new map icon can be found at: " + Run.GAME_ART_PATH + targetName
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the root grid pane object.
     * @return the root grid pane object
     */
    public GridPane getDevMenu() { return devMenu; }

    /**
     * Returns the current tile set view.
     * @return the current tile set view
     */
    public ImageView getTileSetView() { return tileSetView; }

    /**
     * Sets the tileSetView for rendering the tileset original.
     * @param tileSetView the full tile set to be rendered on the dev menu
     */
    public void setTileSetView(ImageView tileSetView) { this.tileSetView = tileSetView; }

    /**
     * Returns the map path text object.
     * @return the map path text object
     */
    public Text getDevMapPath() { return devMapPath; }

    /**
     * Returns the mapList drop box.
     * @return the map list drop box
     */
    public ComboBox<String> getMapList() { return mapList; }

}

