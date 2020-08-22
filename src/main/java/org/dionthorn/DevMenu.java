package org.dionthorn;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;

public class DevMenu extends Stage {
    private Run app; // reference to parent application

    public int SELECTED_TILE_ID = 0; // This is a sentinel value that determines which tileID to be used for drawing
    public int SELECTED_TILE_SET_ID = 0; // This is a sentinel value that determines which tileSetID to be used
    public int SELECTED_MAP_ID = 0; //  This is a sentinel value that determines which mapID to be used
    public boolean EDIT_MODE;

    // Dev menu UI
    private GridPane devMenu; // The main container
    private Text devTileID, devTileSetID, devMapID, devMapPath; // dynamic updating text to show: tileID, tileSetID, etc
    private ScrollPane charInfoPane;
    private CheckBox editMode;
    private CheckBox isFire;
    private CheckBox isImpassable;
    private ArrayList<Integer> fireTileIDs = new ArrayList<Integer>();
    private ArrayList<Integer> impassableTileIDs = new ArrayList<Integer>();
    private ImageView tileSetView; // the currently selected tile set view
    private ComboBox<String> mapList = new ComboBox<>();
    private ComboBox<String> stateList = new ComboBox<>();
    private ArrayList<Text> charInfo, memInfo;
    private Font arial = new Font("Arial", 10);

    public DevMenu(Run app) {
        this.app = app;
        this.setTitle(String.format("DEVMENU %s", app.getProgramVersion()));
        this.setX(0);
        this.setY(0);
        Group devRoot = new Group();
        Scene devRootScene = new Scene(devRoot, 1024, 1024);
        // Cardinal Direction style main UI container
        BorderPane devMainUI = new BorderPane();
        devRoot.getChildren().add(devMainUI);
        // Add Main UI area
        devMenu = new GridPane();
        devMenu.setHgap(10);
        devMenu.setVgap(10);
        // devMenu.setGridLinesVisible(true); // useful for testing alignment
        devMainUI.setCenter(devMenu);
        devMenu.setOnMouseClicked(this::devMenuClicked);

        // devMenu.setGridLinesVisible(true); // DEBUG
        // Add dev controls to devMenu
        devTileID = new Text(String.format("TileID: %s", SELECTED_TILE_ID));
        devTileID.setFont(new Font("Arial", 16));
        GridPane.setConstraints(devTileID, 0, 0);

        Button increaseTileID = new Button("+");
        GridPane.setConstraints(increaseTileID, 2, 0);
        increaseTileID.setOnAction(event -> tileMaxCheck());

        Button decreaseTileID = new Button("-");
        GridPane.setConstraints(decreaseTileID, 3, 0);
        decreaseTileID.setOnAction(event -> tileMinCheck());

        // colIndex 4 is the selected tile image

        editMode = new CheckBox("Edit Mode");
        editMode.setSelected(false);
        EDIT_MODE = false;
        GridPane.setConstraints(editMode, 5, 0);
        editMode.setOnAction(event -> EDIT_MODE = !EDIT_MODE);

        isFire = new CheckBox("isFire");
        GridPane.setConstraints(isFire, 6, 0);
        isFire.setOnAction(event -> setFire());

        isImpassable = new CheckBox("isImpassable");
        GridPane.setConstraints(isImpassable, 7, 0);
        isImpassable.setOnAction(event -> setImpassable());

        tileMetaCheck();

        // rowIndex 1-7 are filled with the tileSetView Image

        devTileSetID = new Text(String.format("TileSet ID: %s", SELECTED_TILE_SET_ID));
        devTileSetID.setFont(new Font("Arial", 16));
        GridPane.setConstraints(devTileSetID, 0, 8, 4, 1);

        Button increaseTileSetID = new Button("+");
        GridPane.setConstraints(increaseTileSetID, 2, 8);
        increaseTileSetID.setOnAction(event -> tileSetMaxCheck());

        Button decreaseTileSetID = new Button("-");
        GridPane.setConstraints(decreaseTileSetID, 3, 8);
        decreaseTileSetID.setOnAction(event -> tileSetMinCheck());

        devMapID = new Text(String.format("Map ID: %s", SELECTED_MAP_ID));
        devMapID.setFont(new Font("Arial", 16));
        GridPane.setConstraints(devMapID, 0, 9, 3, 1);

        Button increaseMapID = new Button("+");
        GridPane.setConstraints(increaseMapID, 2, 9);
        increaseMapID.setOnAction(event -> mapIDMaxCheck());

        Button decreaseMapID = new Button("-");
        GridPane.setConstraints(decreaseMapID, 3, 9);
        decreaseMapID.setOnAction(event -> mapIDMinCheck());

        devMapPath = new Text("null");
        devMapPath.setFont(new Font("Arial", 10));
        GridPane.setConstraints(devMapPath, 0, 10, 2, 1);

        Button saveButton = new Button("Save Map");
        GridPane.setConstraints(saveButton, 2, 10, 3, 1);
        saveButton.setOnAction(event -> app.getGameState().getCurrentMap().saveData());

        GridPane.setConstraints(mapList, 0, 11, 2, 1);

        Button loadMap = new Button("Load Map");
        GridPane.setConstraints(loadMap, 2, 11, 3, 1);
        loadMap.setOnAction(event -> {
            for(Map toLoad: app.getGameState().getMaps()) {
                if(mapList.getSelectionModel().getSelectedItem().equals(toLoad.getPATH())) {
                    app.getGameState().setCurrentMap(toLoad);
                    Player temp = app.getGameState().getPlayerEntity();
                    temp.setCurrentMap(app.getGameState().getCurrentMap(), 0, 0);
                }
            }
        });

        Button devUpdate = new Button("Update");
        GridPane.setConstraints(devUpdate, 0, 12);
        devUpdate.setOnAction(event -> app.update());

        Button devLevelUp = new Button("Level Up");
        devMenu.setConstraints(devLevelUp, 1, 12);
        devLevelUp.setOnAction(event -> {
            if(app.DEBUG_OUTPUT) {
                System.out.println(app.getLastSelectChar());
            }
            if (app.getLastSelectChar() != -1) {
                ((Character) app.getGameState().getEntities().get(app.getLastSelectChar())).levelUp();
            }
        });

        Button memData = new Button("Update resource usage data");
        GridPane.setConstraints(memData, 0, 13);
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
            GridPane.setConstraints(memInfo.get(0), 0, 14, 3, 1);
            devMenu.getChildren().add(memInfo.get(0));
            GridPane.setConstraints(memInfo.get(1), 0, 15, 3, 1);
            devMenu.getChildren().add(memInfo.get(1));
        });

        Button entityInfo = new Button("Update Entity Info");
        GridPane.setConstraints(entityInfo, 0, 16, 3, 1);
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
                    Text tempText = new Text(String.format("[%d] %s (%d, %d) HP:%.2f/%.2f Atk/Def:%.1f/%.1f Critical: %.2f",
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
            GridPane.setConstraints(charInfoPane, 0, 17);
            devMenu.getChildren().add(charInfoPane);
        });

        for(GameState.STATE state: GameState.STATE.values()) {
            stateList.getItems().add(state.name());
        }
        GridPane.setConstraints(stateList, 0, 18);

        Button updateState = new Button("!Update State! CAN BREAK ENGINE");
        GridPane.setConstraints(updateState, 1, 18);
        updateState.setOnAction(event -> {
            if(stateList.getSelectionModel().getSelectedItem() != null && !stateList.getSelectionModel().getSelectedItem().equals("")) {
                String temp = stateList.getSelectionModel().getSelectedItem();
                for(GameState.STATE state: GameState.STATE.values()) {
                    if(state.name().equals(temp)) {
                        app.getGameState().setState(state);
                    }
                }
            }
        });

        devMenu.getChildren().addAll(devTileID, increaseTileID, decreaseTileID, editMode,
                isFire, isImpassable, devUpdate, devLevelUp,
                devTileSetID, increaseTileSetID, decreaseTileSetID,
                devMapID, increaseMapID, decreaseMapID,
                devMapPath, saveButton,
                mapList, loadMap,
                memData, entityInfo,
                stateList, updateState);

        this.setX(0D);
        this.setScene(devRootScene);
        this.show();

        devRootScene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if(key.getCode() == KeyCode.S) {
                // save current map to file (PATH when created or "CUSTOM.{mapNum}.dat")
                app.getGameState().getCurrentMap().saveData();
            } else if(key.getCode() == KeyCode.L) {
                // Need to work on loading (maybe an actual splash screen with menu or something)
            }
        });
    }

    private void devMenuClicked(MouseEvent event) {
        Node clickedNode = event.getPickResult().getIntersectedNode();
        if(clickedNode == tileSetView) {
            // getCellBounds returns the cell size
            Bounds topCell = devMenu.getCellBounds(0,0);
            double heightDiff = topCell.getHeight() + devMenu.getVgap() * 2; // We need to account for the nodes above
            // the tilesetview AND the vertical gap (doubled because v gap is above & below each node)
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
            if(Run.DEBUG_OUTPUT) {
                System.out.println(String.format("Ix=%d, Iy=%d", xCord, yCord)); // This is the (x, y) of the tileset Image
            }

            int tileX = xCord / app.getGameState().getCurrentMap().getTileSize();
            int tileY = yCord / app.getGameState().getCurrentMap().getTileSize();
            if(Run.DEBUG_OUTPUT) {
                System.out.println(String.format("Tx=%d, Ty=%d", tileX, tileY)); // This is the (x, y) of the Tile in the Image
            }
            int maxX = (int) view.getImage().getWidth() / app.getGameState().getCurrentMap().getTileSize();
            if(tileY > 0) {
                tileY = tileY * maxX;
            }
            int tileIDToChange = tileX + tileY;
            // Need to check for if tile is 'blank' or not
            WritableImage toCheck = new WritableImage(app.getGameState().getCurrentMap().getTileSize(), app.getGameState().getCurrentMap().getTileSize());
            PixelReader pr = tileSetView.getImage().getPixelReader();
            int yOffset = (tileY / maxX) * app.getGameState().getCurrentMap().getTileSize();
            int xOffset = tileX * app.getGameState().getCurrentMap().getTileSize();
            for(int yR=0; yR<app.getGameState().getCurrentMap().getTileSize(); yR++) {
                for(int xR=0; xR<app.getGameState().getCurrentMap().getTileSize(); xR++) {
                    toCheck.getPixelWriter().setColor(xR, yR, pr.getColor(xR+xOffset, yR+yOffset));
                }
            }
            if(TileSet.areImagesSame(app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getBlank(), toCheck)) {
                SELECTED_TILE_ID = app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getTotalTiles() - 1; // Sets Tile to Blank
                devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
            } else {
                int blankOffset = 0;
                int[] removedIDs = app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getRemovedTileID();
                for(int id: removedIDs) {
                    if(tileIDToChange >= id) {
                        blankOffset++;
                    }
                }
                SELECTED_TILE_ID = tileIDToChange - blankOffset;
                devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
            }
        }
        tileMetaCheck();
    }

    public void tileMetaCheck() {
        isFire.setSelected(false);
        isImpassable.setSelected(false);
        for(int tileID: app.getGameState().getCurrentMap().getMapFireTileIDs()) {
            if(tileID == SELECTED_TILE_ID) {
                isFire.setSelected(true);
            }
        }
        for(int tileID: app.getGameState().getCurrentMap().getMapImpassableTileIDs()) {
            if(tileID == SELECTED_TILE_ID) {
                isImpassable.setSelected(true);
            }
        }
    }

    public void setFire() {
        if(!isFire.isSelected()) {
            app.getGameState().getCurrentMap().setMapFireTileIDs(SELECTED_TILE_ID, true);
            isFire.setSelected(false);
        } else {
            app.getGameState().getCurrentMap().setMapFireTileIDs(SELECTED_TILE_ID, false);
            isFire.setSelected(true);
        }
        tileMetaCheck();
    }

    public void setImpassable() {
        if(!isImpassable.isSelected()) {
            app.getGameState().getCurrentMap().setMapImpassableTileIDs(SELECTED_TILE_ID, true);
        } else {
            app.getGameState().getCurrentMap().setMapImpassableTileIDs(SELECTED_TILE_ID, false);
        }
        tileMetaCheck();
    }

    private void tileMaxCheck() {
        if(SELECTED_TILE_ID + 1 >= app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getTotalTiles()) {
            System.err.println("TileID cannot exceed TileSet maximum");
        } else {
            SELECTED_TILE_ID++;
            devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
        }
        if(Run.DEBUG_OUTPUT) {
            System.out.println(String.format("SELECTED_TILE_ID: %d", SELECTED_TILE_ID));
        }
    }

    private void tileMinCheck() {
        if(SELECTED_TILE_ID - 1 < 0) {
            System.err.println("TileID cannot exceed map minimum");
        } else {
            SELECTED_TILE_ID--;
            devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
        }
        if(Run.DEBUG_OUTPUT) {
            System.out.println(String.format("SELECTED_TILE_ID: %d", SELECTED_TILE_ID));
        }
    }

    private void tileSetMaxCheck() {
        if(SELECTED_TILE_SET_ID + 1 >= app.getGameState().getCurrentMap().getTileSets().length) {
            System.err.println("TileSetID cannot exceed map maximum");
        } else {
            SELECTED_TILE_SET_ID++;
            if(SELECTED_TILE_ID >= app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getTotalTiles()) {
                SELECTED_TILE_ID = app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getTotalTiles() - 1;
                devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
            }
            devTileSetID.setText(String.format("TileSet ID: %s", SELECTED_TILE_SET_ID));
        }
    }

    private void tileSetMinCheck() {
        if(SELECTED_TILE_SET_ID - 1 < 0) {
            System.err.println("TileSetID cannot exceed map minimum");
        } else {
            SELECTED_TILE_SET_ID--;
            if(SELECTED_TILE_ID >= app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getTotalTiles()) {
                SELECTED_TILE_ID = app.getGameState().getCurrentMap().getTileSet(SELECTED_TILE_SET_ID).getTotalTiles() - 1;
                devTileID.setText(String.format("TileID: %s", SELECTED_TILE_ID));
            }
            devTileSetID.setText(String.format("TileSet ID: %s", SELECTED_TILE_SET_ID));
        }
    }

    private void mapIDMaxCheck() {
        // move up a map ID or generate a new one
        try {
            app.getGameState().setCurrentMap(app.getGameState().getMaps().get(SELECTED_MAP_ID + 1));
            app.getGameState().getPlayerEntity().setCurrentMap(app.getGameState().getCurrentMap(), 0, 0);
            SELECTED_MAP_ID++;
            devMapID.setText(String.format("Map ID: %s", SELECTED_MAP_ID));
            devMapPath.setText(String.format("File Name: %s", app.getGameState().getCurrentMap().getPATH()));
        } catch (Exception e) {
            System.err.println(String.format("Map %d Not Available Creating Random New Map", SELECTED_MAP_ID + 1));
            app.getGameState().getMaps().add(new Map(app.getGameState().getCurrentMap().getTileSetPaths()));
            app.getGameState().setCurrentMap(app.getGameState().getMaps().get(SELECTED_MAP_ID + 1));
            app.getGameState().getPlayerEntity().setCurrentMap(app.getGameState().getCurrentMap(), 0, 0);
            SELECTED_MAP_ID++;
            devMapID.setText(String.format("Map ID: %s", SELECTED_MAP_ID));
            devMapPath.setText(String.format("File Name: %s", app.getGameState().getCurrentMap().getPATH()));
            mapList.getItems().add(app.getGameState().getCurrentMap().getPATH());
            mapList.getSelectionModel().select(SELECTED_MAP_ID);
        }
    }

    private void mapIDMinCheck() {
        // move down a map ID
        if(SELECTED_MAP_ID - 1 < 0) {
            System.err.println("Map ID cannot be less than 0");
        } else {
            try {
                app.getGameState().setCurrentMap(app.getGameState().getMaps().get(SELECTED_MAP_ID - 1));
                app.getGameState().getPlayerEntity().setCurrentMap(app.getGameState().getCurrentMap(), 0, 0);
                SELECTED_MAP_ID--;
                devMapID.setText(String.format("Map ID: %s", SELECTED_MAP_ID));
                devMapPath.setText(String.format("File Name: %s", app.getGameState().getCurrentMap().getPATH()));
            } catch (Exception e) {
                System.err.println(String.format("Map %d Not Available", SELECTED_MAP_ID - 1));
            }
        }
    }

    public GridPane getDevMenu() { return devMenu; }

    public ImageView getTileSetView() { return tileSetView; }

    public void setTileSetView(ImageView tileSetView) { this.tileSetView = tileSetView; }

    public Text getDevMapPath() { return devMapPath; }

    public ComboBox<String> getMapList() { return mapList; }
}

