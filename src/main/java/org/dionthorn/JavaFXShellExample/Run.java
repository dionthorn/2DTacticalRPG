package org.dionthorn.javafxshellexample;

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
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Run extends Application {

    private final String PROGRAM_VERSION = "v1.0";
    private static final int SCREEN_WIDTH = 1024;
    private static final int SCREEN_HEIGHT = 1024;
    private final long FPS = TimeUnit.SECONDS.toNanos(1) / 60;
    private long startTime = System.nanoTime();
    private long currentTime;
    private GraphicsContext gc;

    // This isn't needed for minimal run environment
    private Random rand = new Random();
    private int maxLights = 20;
    private Light[] lights = new Light[maxLights];
    //

    public void render() {
        // This isn't needed for minimal run environment
        gc.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        for(Light light: lights) {
            if(light != null) {
                gc.setFill(Color.WHITE);
                gc.fillRect(light.getX(), light.getY(), 5, 5);
            }
        }
        //
    }

    public void update() {
        // This isn't needed for minimal run environment
        int timeCap = 10;
        for(Light light: lights) {
            if(light != null && light.getLastMoved() <= timeCap) {
                light.setLastMoved(light.getLastMoved() + 1);
            } else if(light != null) {
                light.setX(rand.nextInt(SCREEN_WIDTH - Light.LIGHT_SIZE));
                light.setY(rand.nextInt(SCREEN_HEIGHT - Light.LIGHT_SIZE));
                light.setLastMoved(0);
            }
        }
        //
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
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            int mouseX = (int) mouseEvent.getX();
            int mouseY = (int) mouseEvent.getY();
            System.out.printf("Mouse Clicked At: (%d, %d)%n", mouseX, mouseY);

            // This isn't needed for minimal run environment
            for(int i=0; i<lights.length; i++) {
                if(lights[i] == null) {
                    lights[i] = new Light(mouseX, mouseY);
                    break;
                }
            }
            //
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
