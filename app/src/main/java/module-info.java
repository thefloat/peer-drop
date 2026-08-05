module com.peerdrop.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires jdk.httpserver;
    requires java.net.http;
    requires java.desktop;

    opens com.peerdrop.desktop.view.controller to javafx.fxml;
    opens com.peerdrop.desktop.view to javafx.fxml, javafx.graphics;
    opens com.peerdrop.desktop.service to com.google.gson;

    exports com.peerdrop.desktop;
    exports com.peerdrop.desktop.model;
}