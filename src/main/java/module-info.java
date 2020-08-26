module MavenTactical {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires java.logging;
    requires java.desktop;
    requires javafx.swing;

    exports org.dionthorn to javafx.graphics;
    exports org.dionthorn.JavaFXShellExample to javafx.graphics;
}