package org.dionthorn.JavaFXShellExample;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.concurrent.TimeUnit;

public class Run_Minimal extends Application {

    private final String PROGRAM_VERSION = "v1.0";
    private static final int SCREEN_WIDTH = 1024;
    private static final int SCREEN_HEIGHT = 1024;
    private final long FPS = TimeUnit.SECONDS.toNanos(1) / 60;
    private long startTime = System.nanoTime();
    private long currentTime;
    private GraphicsContext gc;

    public void render() {

    }

    public void update() {

    }

    @Override
    public void start(Stage primaryStage) {
        // Provide basic contexts and setup the window
        primaryStage.setTitle("Example Shell " + PROGRAM_VERSION);
        primaryStage.setResizable(false);
        Group rootGroup = new Group();
        Scene rootScene = new Scene(rootGroup, SCREEN_WIDTH, SCREEN_HEIGHT, Color.BLACK);
        primaryStage.sizeToScene();
        Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        rootGroup.getChildren().add(canvas);

        // Add User Input Handling here
        rootScene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            System.out.println("Key Pressed: " + key.getCode());
            if (key.getCode() == KeyCode.ESCAPE) {
                primaryStage.close();
                System.exit(0);
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            int mouseX = (int) mouseEvent.getX();
            int mouseY = (int) mouseEvent.getY();
            System.out.printf("Mouse Clicked At: (%d, %d)%n", mouseX, mouseY);
        });

        // Setup animator
        primaryStage.setScene(rootScene);
        primaryStage.show();
        AnimationTimer animator = new AnimationTimer() {
            @Override
            public void handle(long arg0) {
                currentTime = System.nanoTime();
                if (FPS <= (currentTime - startTime)) {
                    update();
                    render();
                    startTime = currentTime;
                }
            }
        };
        animator.start();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
